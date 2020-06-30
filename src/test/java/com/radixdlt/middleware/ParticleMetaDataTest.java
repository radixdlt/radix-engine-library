/*
 *
 *  * (C) Copyright 2020 Radix DLT Ltd
 *  *
 *  * Radix DLT Ltd licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except in
 *  * compliance with the License.  You may obtain a copy of the
 *  * License at
 *  *
 *  *  http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *  * either express or implied.  See the License for the specific
 *  * language governing permissions and limitations under the License.
 *
 */

package com.radixdlt.middleware;

import org.junit.Test;


import static org.junit.Assert.assertEquals;

public class ParticleMetaDataTest {

	@Test
	public void test_no_data_single_transfer_small_amount_no_change() {
		String atomCborHex = "bf686d65746144617461bf6974696d657374616d706d31353931383633333439393231ff6e7061727469636c6547726f75707381bf697061727469636c657382bf687061727469636c65bf676164647265737358270401026d5e07cfde5df84b5ef884b629d28d15b0f6c66be229680699767cd57c618288c5bb6ef266616d6f756e74582105000000000000000000000000000000000000000000000000000000000000000a6c64657374696e6174696f6e7381510205f1e0f9fa6f2922f14827be92d0a2a06b6772616e756c61726974795821050000000000000000000000000000000000000000000000000000000000000001656e6f6e63651b4e992519f0d2bdb966706c616e636b1a0195bf4f6a73657269616c697a6572782472616469782e7061727469636c65732e7472616e736665727261626c655f746f6b656e737818746f6b656e446566696e6974696f6e5265666572656e6365583b062f39664d4541314c4e5332416d7253784169316f44584b664d633352476b367a6f585a4a39684b6d776b755a4e4a6e4431566e712f5a454c44416776657273696f6e1864ff6a73657269616c697a65727372616469782e7370756e5f7061727469636c65647370696e206776657273696f6e1864ffbf687061727469636c65bf67616464726573735827040102147ef2bc3cffdbebf6a6bc1057a5de87702b2f99adc7b4a0e083ec4397c3487f78aa1ec666616d6f756e74582105000000000000000000000000000000000000000000000000000000000000000a6c64657374696e6174696f6e73815102d16cd8258746903b4f8fca1836d4aeb56b6772616e756c61726974795821050000000000000000000000000000000000000000000000000000000000000001656e6f6e63653b081d664c849f4d2066706c616e636b1a0195bf4f6a73657269616c697a6572782472616469782e7061727469636c65732e7472616e736665727261626c655f746f6b656e737818746f6b656e446566696e6974696f6e5265666572656e6365583b062f39664d4541314c4e5332416d7253784169316f44584b664d633352476b367a6f585a4a39684b6d776b755a4e4a6e4431566e712f5a454c44416776657273696f6e1864ff6a73657269616c697a65727372616469782e7370756e5f7061727469636c65647370696e016776657273696f6e1864ff6a73657269616c697a65727472616469782e7061727469636c655f67726f75706776657273696f6e1864ff6776657273696f6e1864ff";
		String metaDataHexString = ParticleMetaData.hexStringFromAtom(atomCborHex);
		String expectedMetaDataHex = "01e600290216002302ae002602ee003d";
		assertEquals(expectedMetaDataHex, metaDataHexString);
	}
}