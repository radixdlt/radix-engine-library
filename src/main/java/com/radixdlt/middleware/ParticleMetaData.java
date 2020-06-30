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

import com.google.common.collect.ImmutableList;
import com.radixdlt.DefaultSerialization;
import com.radixdlt.atommodel.Atom;
import com.radixdlt.atommodel.tokens.TransferrableTokensParticle;
import com.radixdlt.constraintmachine.Particle;
import com.radixdlt.constraintmachine.Spin;
import com.radixdlt.serialization.ClassScanningSerializerIds;
import com.radixdlt.serialization.DsonOutput;
import com.radixdlt.serialization.Serialization;
import com.radixdlt.serialization.SerializationException;
import com.radixdlt.serialization.SerializerId2;
import com.radixdlt.utils.Bytes;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ParticleMetaData {

	private ByteInterval addressField;
	private ByteInterval amountField;
	private ByteInterval serializerField;
	private ByteInterval tokenDefinitionReferenceField;

	public ParticleMetaData(
			ByteInterval addressField,
			ByteInterval amountField,
			ByteInterval serializerField,
			ByteInterval tokenDefinitionReferenceField
	) {
		this.addressField = addressField;
		this.amountField = amountField;
		this.serializerField = serializerField;
		this.tokenDefinitionReferenceField = tokenDefinitionReferenceField;
	}

	public static ParticleMetaData nonTTP(ByteInterval serializerByteInterval) {
		return new ParticleMetaData(
				ByteInterval.zero(),
				ByteInterval.zero(),
				serializerByteInterval,
				ByteInterval.zero()
		);
	}

	public static final class ByteInterval {
		private short startsAt;
		private short byteCount;

		private ByteInterval(short startsAt, short byteCount) {
			this.startsAt = startsAt;
			this.byteCount = byteCount;
		}

		public ByteInterval(int startsAt, int byteCount) {
			this((short) startsAt, (short) byteCount);
			assert(byteCount < 65536);
			assert(startsAt < 65536);
		}

		public static ByteInterval inString(int stringIndex, int chars) {
			return new ByteInterval(stringIndex/2, chars/2);
		}

		public static ByteInterval zero() {
			return new ByteInterval((short) 0, (short) 0);
		}

		public byte[] getBytes() {
			ByteBuffer buffer = ByteBuffer.allocate(4);
			buffer.putShort(startsAt);
			buffer.putShort(byteCount);
			return buffer.array();
		}

		public boolean isEmpty() {
			return byteCount == 0;
		}
	}

	public boolean isTTP() {
		return !addressField.isEmpty() && !amountField.isEmpty() && !tokenDefinitionReferenceField.isEmpty();
	}

	public byte[] getBytes() {
		ByteBuffer buffer = ByteBuffer.allocate(16);
		buffer.put(addressField.getBytes());
		buffer.put(amountField.getBytes());
		buffer.put(serializerField.getBytes());
		buffer.put(tokenDefinitionReferenceField.getBytes());
		return buffer.array();
	}

	private enum ParticleField {
		ADDRESS, AMOUNT, SERIALIZER, TOKEN_DEFINITION_REFERENCE;
	}

//	private static void add(int increment, Short to) {
//		to = Short.valueOf((short) (to.intValue() + increment));
//	}
//
//	private static Short shortFrom(int a, int b) {
//		return Short.valueOf((short) (a + b));
//	}
//
//	private static Short shortFrom(int a, Short shorT) {
//		return Short.valueOf((short) (a + shorT.intValue()));
//	}


	private static ByteInterval byteInterval(
		ParticleField of,
		Particle in,
		String spunUpParticleCBORHex,
		int offsetOfParticleInAtomInChars
	) {

		ParticleField field = of;
		Particle particle = in;
//		assert particle instanceof SerializerId2;

		if (!(particle instanceof TransferrableTokensParticle) && field != ParticleField.SERIALIZER) {
			throw new RuntimeException("Bad state");
		}

		Serialization serializer = DefaultSerialization.getInstance();
		TransferrableTokensParticle ttp = null;
		if (particle instanceof TransferrableTokensParticle) {
			ttp = (TransferrableTokensParticle) particle;
		}

		final byte[] fieldCBORBytes;
		try {
			switch (field) {
				case ADDRESS:
					fieldCBORBytes = serializer.toDson(ttp.getAddress(), DsonOutput.Output.HASH);
					break;
				case AMOUNT:
					fieldCBORBytes = serializer.toDson(ttp.getAmount(), DsonOutput.Output.HASH);
					break;
				case SERIALIZER:
//					SerializerId2 serializerOwner = (SerializerId2) particle;

					String serializerForParticle = serializer.getIdForClass(particle.getClass());
					System.out.println(String.format("EPIC found serializer: %s", serializerForParticle));

					fieldCBORBytes = serializer.toDson(serializerForParticle, DsonOutput.Output.HASH);
					break;
				case TOKEN_DEFINITION_REFERENCE:
					fieldCBORBytes = serializer.toDson(ttp.getTokDefRef(), DsonOutput.Output.HASH);
					break;
				default: throw new RuntimeException("Unhandled field");
			}
		} catch (SerializationException e) {
			throw new RuntimeException("Failed to CBOR encode");
		}

		String fieldCBORHex = Bytes.toHexString(fieldCBORBytes);
		int fieldStartInParticleInChars = spunUpParticleCBORHex.indexOf(fieldCBORHex);
		int fieldStartInAtomInChars = offsetOfParticleInAtomInChars + fieldStartInParticleInChars;
		System.out.println(String.format("fieldCBORHex: %s", fieldCBORHex));
		System.out.println(String.format("spunUpParticleCBORHex: %s", spunUpParticleCBORHex));
		System.out.println(String.format("offsetOfParticleInAtomInChars: %d", offsetOfParticleInAtomInChars));
		System.out.println(String.format("fieldStartInParticleInChars: %d", fieldStartInParticleInChars));
		System.out.println(String.format("fieldStartInAtomInChars: %d", fieldStartInAtomInChars));
		int fieldSizeInChars = fieldCBORHex.length();

		return ByteInterval.inString(fieldStartInAtomInChars, fieldSizeInChars);
	}

	public static String hexStringFromAtom(String atomCborHexString) {
		byte[] atomCborBytes = Bytes.fromHexString(atomCborHexString);
		Atom atom = null;
		try {
			atom = DefaultSerialization.getInstance().fromDson(atomCborBytes, Atom.class);
		} catch (SerializationException e) {
			throw new RuntimeException("Failed to decode bytes to Atom, error: " + e);
		}
		return ParticleMetaData.hexStringFromAtom(atom);
	}

	public static String hexStringFromAtom(Atom atom) {
		return Bytes.toHexString(byteArrayFromAtom(atom));
	}

	public static byte[] byteArrayFromAtom(Atom atom) {
		List<ParticleMetaData> particleMetaData = null;
		try {
			particleMetaData = ParticleMetaData.fromAtom(atom);
		} catch (SerializationException e) {
			throw new RuntimeException("Failed to create metadata");
		}
		int upParticleCount = atom.particles(Spin.UP).collect(Collectors.toList()).size();
		ByteBuffer buffer = ByteBuffer.allocate(16 * upParticleCount);
		for (ParticleMetaData metaData : particleMetaData) {
			buffer.put(metaData.getBytes());
		}
		return buffer.array();
	}

	public static List<ParticleMetaData> fromAtom(Atom atom) throws SerializationException {
		byte[] atomCborBytes = DefaultSerialization.getInstance().toDson(atom, DsonOutput.Output.HASH);
		String atomCborHex = Bytes.toHexString(atomCborBytes);

		System.out.println(String.format("🔮Atom CBOR hex: %s\n🧩", atomCborHex));

		List<SpunParticle> spunUpParticles = atom.spunParticles()
				.filter(p -> p.getSpin() == Spin.UP)
				.collect(Collectors.toList());

		ArrayList<ParticleMetaData> metaDataList = new ArrayList<>();

		int offsetInAtomInChars = 0;
		for (SpunParticle spunUpParticle : spunUpParticles) {
			byte[] spunUpParticleCBORBytes = DefaultSerialization.getInstance().toDson(spunUpParticle, DsonOutput.Output.ALL);
			String spunUpParticleCBORHex = Bytes.toHexString(spunUpParticleCBORBytes);

			System.out.println(String.format("Searching for needle=\n<\n%s\n>\nin haystack=\n<\n%s\n>\n", spunUpParticleCBORHex, atomCborHex));

			int offsetOfParticleInAtomInChars = atomCborHex.indexOf(spunUpParticleCBORHex) + offsetInAtomInChars;
			System.out.println(String.format("FOR LOOP offsetOfParticleInAtomInChars: %d", offsetOfParticleInAtomInChars));
			Particle upParticle = spunUpParticle.getParticle();

			final ParticleMetaData metaData;
			if (upParticle instanceof TransferrableTokensParticle) {

				Function<ParticleField, ByteInterval> makeNextByteInterval = (field) -> {
					ByteInterval newInterval = byteInterval(
							field,
							upParticle,
							spunUpParticleCBORHex,
							offsetOfParticleInAtomInChars
					);

					return newInterval;
				};

				metaData = new ParticleMetaData(
					makeNextByteInterval.apply(ParticleField.ADDRESS),
					makeNextByteInterval.apply(ParticleField.AMOUNT),
					makeNextByteInterval.apply(ParticleField.SERIALIZER),
					makeNextByteInterval.apply(ParticleField.TOKEN_DEFINITION_REFERENCE)
				);

				assert(metaData.isTTP());

			} else {
				ByteInterval serializerByteInterval = byteInterval(
					ParticleField.SERIALIZER,
					upParticle,
					spunUpParticleCBORHex,
					offsetOfParticleInAtomInChars
				);

				metaData = ParticleMetaData.nonTTP(serializerByteInterval);

				assert(!metaData.isTTP());
			}

			metaDataList.add(metaData);
			offsetInAtomInChars = offsetOfParticleInAtomInChars + spunUpParticleCBORHex.length();
			atomCborHex = atomCborHex.replaceFirst(spunUpParticleCBORHex, "");
		}

		return ImmutableList.copyOf(metaDataList);
	}
}
