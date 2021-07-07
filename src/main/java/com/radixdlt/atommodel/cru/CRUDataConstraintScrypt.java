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

package com.radixdlt.atommodel.cru;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.TypeToken;
import com.radixdlt.atomos.ConstraintScrypt;
import com.radixdlt.atomos.ParticleDefinition;
import com.radixdlt.atomos.Result;
import com.radixdlt.atomos.SysCalls;
import com.radixdlt.constraintmachine.TransitionProcedure;
import com.radixdlt.constraintmachine.TransitionToken;
import com.radixdlt.constraintmachine.UsedCompute;
import com.radixdlt.constraintmachine.VoidUsedData;
import com.radixdlt.constraintmachine.WitnessValidator;
import com.radixdlt.constraintmachine.WitnessValidator.WitnessValidatorResult;
import com.radixdlt.identifiers.RRI;
import com.radixdlt.identifiers.RadixAddress;

/**
 * Scrypt which defines how CRU data is managed.
 */
public class CRUDataConstraintScrypt implements ConstraintScrypt {

	@Override
	public void main(SysCalls os) {
		os.registerParticle(
			CRUDataParticle.class,
			ParticleDefinition.<CRUDataParticle>builder()
				.staticValidation(checkAddress(CRUDataConstraintScrypt::getAddress))
				.singleAddressMapper(CRUDataConstraintScrypt::getAddress)
				.rriMapper(CRUDataParticle::rri)
				.build()
		);

		// Create with RRIParticle
		os.createTransitionFromRRI(CRUDataParticle.class);

		// Update with new CRUDataParticle
		os.createTransition(
			new TransitionToken<>(CRUDataParticle.class, TypeToken.of(VoidUsedData.class), CRUDataParticle.class, TypeToken.of(VoidUsedData.class)),
			new CRUTransitionProcedure()
		);
	}

	private static RadixAddress getAddress(CRUDataParticle p) {
		RRI rri = p.rri();
		return rri == null ? null : rri.getAddress();
	}

	// check that the given RRI / address isn't null
	private static Function<CRUDataParticle, Result> checkAddress(Function<CRUDataParticle, RadixAddress> addressMapper) {
		return p -> addressMapper.apply(p) == null ? Result.error("RRI is invalid") : Result.success();
	}

	@VisibleForTesting
	static class CRUTransitionProcedure implements TransitionProcedure<CRUDataParticle, VoidUsedData, CRUDataParticle, VoidUsedData> {

		@Override
		public Result precondition(CRUDataParticle inputParticle, VoidUsedData inputUsed, CRUDataParticle outputParticle, VoidUsedData outputUsed) {
			// ensure transition is between CRU particles with same RRI
			if (!Objects.equals(inputParticle.rri(), outputParticle.rri())) {
				return Result.error(String.format(
					"CRU RRIs do not match: %s != %s",
					inputParticle.rri(), outputParticle.rri()
				));
			}

			// ensure serialno is incremented on update
			if (inputParticle.serialno() + 1 != outputParticle.serialno()) {
				return Result.error(
					String.format(
						"output serialno must be input serialno + 1, but %s != %s + 1",
						outputParticle.serialno(), inputParticle.serialno()
					)
				);
			}
			return Result.success();
		}

		@Override
		public UsedCompute<CRUDataParticle, VoidUsedData, CRUDataParticle, VoidUsedData> inputUsedCompute() {
			return (input, inputUsed, output, outputUsed) -> Optional.empty();
		}

		@Override
		public UsedCompute<CRUDataParticle, VoidUsedData, CRUDataParticle, VoidUsedData> outputUsedCompute() {
			return (input, inputUsed, output, outputUsed) -> Optional.empty();
		}

		@Override
		public WitnessValidator<CRUDataParticle> inputWitnessValidator() {
			// verify that the transition was signed by the owner
			return (i, meta) -> {
				RadixAddress address = getAddress(i);
				return address != null && meta.isSignedBy(address.getPublicKey())
					? WitnessValidatorResult.success()
					: WitnessValidatorResult.error(String.format("CRU %s not signed", i.rri()));
			};
		}

		@Override
		public WitnessValidator<CRUDataParticle> outputWitnessValidator() {
			// input.rri == output.rri, so no need to check signature twice
			return (i, meta) -> WitnessValidatorResult.success();
		}
	}
}
