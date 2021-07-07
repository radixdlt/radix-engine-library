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

import com.radixdlt.TestSetupUtils;
import com.radixdlt.crypto.ECKeyPair;

import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.radixdlt.identifiers.RRI;
import com.radixdlt.identifiers.RadixAddress;
import com.radixdlt.serialization.SerializeObjectEngine;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * JSON Serialization round trip of {@link CRUDataParticle} object.
 */
public class CRUDataParticleSerializationTest extends SerializeObjectEngine<CRUDataParticle> {
	private static final String NAME = "TEST";
	private static final ECKeyPair keyPair;
	private static final RadixAddress address;
	private static final RRI rri;

	static {
		keyPair = ECKeyPair.generateNew();
		address = new RadixAddress((byte) 123, keyPair.getPublicKey());
		rri = RRI.of(address, NAME);
	}

    public CRUDataParticleSerializationTest() {
        super(CRUDataParticle.class, CRUDataParticleSerializationTest::get);
    }

    @BeforeClass
    public static void startRRIParticleSerializationTest() {
        TestSetupUtils.installBouncyCastleProvider();
    }

    @Test
    public void testGetters() {
    	CRUDataParticle p = get();
    	assertEquals(rri, p.rri());
    	assertEquals(1234L, p.serialno());
    }

    @Test
    public void testToString() {
    	String s = get().toString();
    	assertThat(s, containsString(CRUDataParticle.class.getSimpleName()));
    	assertThat(s, containsString(rri.toString()));
    }

    private static CRUDataParticle get() {
    	return new CRUDataParticle(rri, 1234L, new byte[10]);
    }
}
