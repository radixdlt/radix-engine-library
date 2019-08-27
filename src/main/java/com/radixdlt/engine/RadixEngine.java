package com.radixdlt.engine;

import com.google.common.collect.ImmutableSet;
import com.radixdlt.atomos.Result;
import com.radixdlt.atoms.DataPointer;
import com.radixdlt.atoms.Particle;
import com.radixdlt.atoms.Spin;
import com.radixdlt.atoms.SpunParticle;
import com.radixdlt.constraintmachine.CMErrorCode;
import com.radixdlt.constraintmachine.CMInstruction;
import com.radixdlt.constraintmachine.CMError;
import com.radixdlt.constraintmachine.CMMicroInstruction;
import com.radixdlt.constraintmachine.ConstraintMachine;
import com.radixdlt.store.CMStore;
import com.radixdlt.store.EngineStore;
import com.radixdlt.store.SpinStateMachine;
import com.radixdlt.store.SpinStateTransitionValidator;
import com.radixdlt.store.SpinStateTransitionValidator.TransitionCheckResult;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Top Level Class for the Radix Engine, a real-time, shardable, distributed state machine.
 */
public final class RadixEngine<T extends RadixEngineAtom> {

	private interface EngineAction<U extends RadixEngineAtom> {
	}

	private final class DeleteAtom<U extends RadixEngineAtom> implements EngineAction<U> {
		private final U cmAtom;
		DeleteAtom(U cmAtom) {
			this.cmAtom = cmAtom;
		}
	}

	private final class StoreAtom<U extends RadixEngineAtom> implements EngineAction<U> {
		private final U cmAtom;
		private final AtomEventListener<U> listener;

		StoreAtom(U cmAtom, AtomEventListener<U> listener) {
			this.cmAtom = cmAtom;
			this.listener = listener;
		}
	}

	private final ConstraintMachine constraintMachine;
	private final CMStore virtualizedCMStore;

	private final EngineStore<T> engineStore;
	private final CopyOnWriteArrayList<AtomEventListener<T>> atomEventListeners = new CopyOnWriteArrayList<>();
	private final CopyOnWriteArrayList<CMSuccessHook<T>> cmSuccessHooks = new CopyOnWriteArrayList<>();
	private	final BlockingQueue<EngineAction<T>> commitQueue = new LinkedBlockingQueue<>();
	private final Thread stateUpdateEngine;

	public RadixEngine(
		ConstraintMachine constraintMachine,
		EngineStore<T> engineStore
	) {
		this.constraintMachine = constraintMachine;
		this.virtualizedCMStore = constraintMachine.getVirtualStore().apply(engineStore);
		this.engineStore = engineStore;
		this.stateUpdateEngine = new Thread(this::run);
		this.stateUpdateEngine.setDaemon(true);
		this.stateUpdateEngine.setName("Radix Engine");
	}

	private void run() {
		while (true) {
			try {
				final EngineAction<T> action = this.commitQueue.take();
				if (action instanceof StoreAtom) {
					StoreAtom<T> storeAtom = (StoreAtom<T>) action;
					stateCheckAndStore(storeAtom);
				} else if (action instanceof DeleteAtom) {
					DeleteAtom<T> deleteAtom = (DeleteAtom<T>) action;
					engineStore.deleteAtom(deleteAtom.cmAtom);
				}
			} catch (InterruptedException e) {
				// Just exit if we are interrupted
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	// TODO: temporary interface, remove in favor of reactive-streams
	public int getCommitQueueSize() {
		return commitQueue.size();
	}

	public void start() {
		stateUpdateEngine.start();
	}

	public void addCMSuccessHook(CMSuccessHook<T> hook) {
		this.cmSuccessHooks.add(hook);
	}

	public void addAtomEventListener(AtomEventListener<T> acceptor) {
		this.atomEventListeners.add(acceptor);
	}

	public void delete(T cmAtom) {
		this.commitQueue.add(new DeleteAtom<>(cmAtom));
	}

	// TODO use reactive interface
	public void store(T cmAtom, AtomEventListener<T> atomEventListener) {
		Objects.requireNonNull(cmAtom);
		Objects.requireNonNull(atomEventListener);

		final Optional<CMError> error = constraintMachine.validate(cmAtom.getCMInstruction());
		if (error.isPresent()) {
			atomEventListener.onCMError(cmAtom, ImmutableSet.of(error.get()));
			this.atomEventListeners.forEach(acceptor -> acceptor.onCMError(cmAtom, ImmutableSet.of(error.get())));
			return;
		}

		for (CMSuccessHook<T> hook : cmSuccessHooks) {
			Result hookResult = hook.hook(cmAtom);
			if (hookResult.isError()) {
				CMError cmError = new CMError(DataPointer.ofAtom(), CMErrorCode.HOOK_ERROR, null, hookResult.getErrorMessage());
				atomEventListener.onCMError(cmAtom, ImmutableSet.of(cmError));
				this.atomEventListeners.forEach(acceptor -> acceptor.onCMError(cmAtom, ImmutableSet.of(cmError)));
				return;
			}
		}

		this.commitQueue.add(new StoreAtom<>(cmAtom, atomEventListener));

		atomEventListener.onCMSuccess(cmAtom);
		this.atomEventListeners.forEach(acceptor -> acceptor.onCMSuccess(cmAtom));
	}

	private void stateCheckAndStore(StoreAtom<T> storeAtom) {
		final T cmAtom = storeAtom.cmAtom;
		final CMInstruction cmInstruction = cmAtom.getCMInstruction();

		for (CMMicroInstruction microInstruction : cmInstruction.getMicroInstructions()) {
			if (!microInstruction.isCheckSpin()) {
				continue;
			}

			// First spun is the only one we need to check
			final Spin checkSpin = microInstruction.getCheckSpin();
			final Spin nextSpin = SpinStateMachine.next(checkSpin);
			final Particle particle = microInstruction.getParticle();
			final TransitionCheckResult spinCheck = SpinStateTransitionValidator.checkParticleTransition(
				particle,
				nextSpin,
				virtualizedCMStore
			);

			//if (spinCheckResult == TransitionCheckResult.MISSING_STATE_FROM_UNSUPPORTED_SHARD) {
			// Could be missing state needed from other shards. This is okay.
			// TODO: Log
			//}

			if (spinCheck == TransitionCheckResult.ILLEGAL_TRANSITION_TO || spinCheck == TransitionCheckResult.MISSING_STATE) {
				throw new IllegalStateException("Should not be here. This should be caught by Constraint Machine Stateless validation.");
			}

			if (spinCheck == TransitionCheckResult.CONFLICT) {
				final SpunParticle issueParticle = SpunParticle.of(particle, nextSpin);

				// TODO: Refactor so that two DB fetches aren't required to get conflicting atoms
				// TODO Because we're checking SpunParticles I understand there can only be one of
				// them in store as they are unique.
				//
				// Modified StateProviderFromStore.getAtomsContaining to be singular based on the
				// above assumption.
				engineStore.getAtomContaining(issueParticle, conflictAtom -> {
					storeAtom.listener.onStateConflict(cmAtom, issueParticle, conflictAtom);
					atomEventListeners.forEach(listener -> listener.onStateConflict(cmAtom, issueParticle, conflictAtom));
				});

				return;
			}

			if (spinCheck == TransitionCheckResult.MISSING_DEPENDENCY)  {
				SpunParticle issueParticle = SpunParticle.of(particle, nextSpin);

				storeAtom.listener.onStateMissingDependency(cmAtom, issueParticle);
				atomEventListeners.forEach(listener -> listener.onStateMissingDependency(cmAtom, issueParticle));
				return;
			}
		}

		engineStore.storeAtom(cmAtom);
		storeAtom.listener.onStateStore(cmAtom);
		atomEventListeners.forEach(listener -> listener.onStateStore(cmAtom));
	}
}
