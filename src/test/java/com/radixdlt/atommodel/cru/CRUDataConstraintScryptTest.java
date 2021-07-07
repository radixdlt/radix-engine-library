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

import com.radixdlt.atommodel.cru.CRUDataConstraintScrypt.CRUTransitionProcedure;
import com.radixdlt.atomos.CMAtomOS;
import com.radixdlt.atomos.Result;
import com.radixdlt.constraintmachine.Particle;
import com.radixdlt.constraintmachine.WitnessData;
import com.radixdlt.crypto.ECKeyPair;
import com.radixdlt.identifiers.RRI;
import com.radixdlt.identifiers.RadixAddress;
import org.junit.Before;
import org.junit.Test;

import java.util.function.Function;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CRUDataConstraintScryptTest {
	private Function<Particle, Result> staticCheck;

	@Before
	public void initializeConstraintScrypt() {
		CRUDataConstraintScrypt tokensConstraintScrypt = new CRUDataConstraintScrypt();
		CMAtomOS cmAtomOS = new CMAtomOS();
		cmAtomOS.load(tokensConstraintScrypt);
		staticCheck = cmAtomOS.buildParticleStaticCheck();
	}

	@Test
	public void when_validating_with_no_address__result_has_error() {
		CRUDataParticle particle = mock(CRUDataParticle.class);
		assertThat(staticCheck.apply(particle).getErrorMessage()).contains("rri");
	}

	@Test
	public void when_validating_with_mismatching_addresses__result_has_error() {
		CRUTransitionProcedure procedure = new CRUTransitionProcedure();
		CRUDataParticle input = create(makeAddress(), 0);
		CRUDataParticle output = create(makeAddress(), 1);
		assertThat(procedure.precondition(input, null, output, null).getErrorMessage())
			.contains("RRIs do not match");
	}

	@Test
	public void when_validating_cru_update_with_equal_serialno__result_has_error() {
		assertThat(testWithSerialnos(0, 0).getErrorMessage()).contains("serialno");
	}

	@Test
	public void when_validating_cru_update_with_skipped_serialno__result_has_error() {
		assertThat(testWithSerialnos(0, 2).getErrorMessage()).contains("serialno");
	}

	@Test
	public void when_validating_cru_update_with_decreasing_serialno__result_has_error() {
		assertThat(testWithSerialnos(1, 0).getErrorMessage()).contains("serialno");
	}

	@Test
	public void when_validating_validator_registration_with_valid_serialno__result_is_success() {
		assertThat(testWithSerialnos(0, 1).isSuccess()).isTrue();
	}

	@Test
	public void when_validating_without_signature__result_has_error() {
		CRUTransitionProcedure procedure = new CRUTransitionProcedure();
		CRUDataParticle input = create(makeAddress(), 0);
		WitnessData witnessData = mock(WitnessData.class);
		when(witnessData.isSignedBy(any())).thenReturn(false);
		assertThat(procedure.inputWitnessValidator().validate(input, witnessData).getErrorMessage()).contains("not signed");
	}

	private RadixAddress makeAddress() {
		return new RadixAddress((byte) 0, ECKeyPair.generateNew().getPublicKey());
	}

	private Result testWithSerialnos(int inputSerial, int outputSerial) {
		CRUTransitionProcedure procedure = new CRUTransitionProcedure();
		RadixAddress address = makeAddress();
		CRUDataParticle input = create(address, inputSerial);
		CRUDataParticle output = create(address, outputSerial);
		return procedure.precondition(input, null, output, null);
	}

	private CRUDataParticle create(RadixAddress address, long serialno) {
		RRI rri = RRI.of(address, "TEST");
		return new CRUDataParticle(rri, serialno, new byte[10]);
	}
}
