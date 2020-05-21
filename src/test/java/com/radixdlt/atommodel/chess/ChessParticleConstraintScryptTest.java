/*
 * (C) Copyright 2020 Radix DLT Ltd
 *
 * Radix DLT Ltd licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.radixdlt.atommodel.chess;

import com.radixdlt.atomos.CMAtomOS;
import com.radixdlt.atomos.Result;
import com.radixdlt.constraintmachine.Particle;
import com.radixdlt.identifiers.RadixAddress;
import com.radixdlt.store.CMStore;
import com.radixdlt.store.CMStores;
import org.junit.Before;
import org.junit.Test;

import java.util.Objects;
import java.util.function.Function;

import static org.mockito.Mockito.mock;


public class ChessParticleConstraintScryptTest {
	private Function<Particle, Result> staticCheck;
	private CMStore virtualisedStore;

	@Before
	public void initializeConstraintScrypt() {
		ChessParticleConstraintScrypt tokensConstraintScrypt = new ChessParticleConstraintScrypt();
		CMAtomOS cmAtomOS = new CMAtomOS();
		cmAtomOS.load(tokensConstraintScrypt);
		staticCheck = cmAtomOS.buildParticleStaticCheck();
		virtualisedStore = cmAtomOS.buildVirtualLayer().apply(CMStores.empty());
	}

	@Test
	public void when_checking_message_without_bytes__result_is_error() {

	}

	private class BoardBuilder {
		private String boardStateFen = mock(String.class);
		private ChessBoardParticle.GameState gameState = mock(ChessBoardParticle.GameState.class);
		private RadixAddress gameAddress = mock(RadixAddress.class);
		private RadixAddress whiteAddress = mock(RadixAddress.class);
		private RadixAddress blackAddress = mock(RadixAddress.class);
		private String gameId = mock(String.class);
		private long nonce = 0;
		private boolean lastMoveWhite;
		private String lastMove = mock(String.class);

		private BoardBuilder() {
		}
		
		private BoardBuilder boardStateFen(String boardStateFen) {
			this.boardStateFen = Objects.requireNonNull(boardStateFen);
			return this;
		}
		
		private BoardBuilder gameState(ChessBoardParticle.GameState gameState) {
			this.gameState = Objects.requireNonNull(gameState);
			return this;
		}
		
		private BoardBuilder gameAddress(RadixAddress gameAddress) {
			this.gameAddress = Objects.requireNonNull(gameAddress);
			return this;
		}

		private BoardBuilder whiteAddress(RadixAddress whiteAddress) {
			this.whiteAddress = Objects.requireNonNull(whiteAddress);
			return this;
		}
		
		private BoardBuilder blackAddress(RadixAddress blackAddress) {
			this.blackAddress = Objects.requireNonNull(blackAddress);
			return this;
		}

		private BoardBuilder gameId(String gameId) {
			this.gameId = Objects.requireNonNull(gameId);
			return this;
		}

		private BoardBuilder nonce(long nonce) {
			this.nonce = nonce;
			return this;
		}

		private BoardBuilder lastMoveWhite(boolean lastMoveWhite) {
			this.lastMoveWhite = lastMoveWhite;
			return this;
		}

		private BoardBuilder lastMove(String lastMove) {
			this.lastMove = lastMove;
			return this;
		}

		private ChessBoardParticle build() {
			return new ChessBoardParticle(
				this.boardStateFen,
				this.gameState,
				this.gameAddress,
				this.whiteAddress,
				this.blackAddress,
				this.gameId,
				this.nonce,
				this.lastMoveWhite,
				this.lastMove
			);
		}
	}
}
