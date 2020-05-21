package com.radixdlt.atommodel.chess;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.radixdlt.DefaultSerialization;
import com.radixdlt.constraintmachine.Particle;
import com.radixdlt.identifiers.RadixAddress;
import com.radixdlt.serialization.DsonOutput;
import com.radixdlt.serialization.Serialization;
import com.radixdlt.serialization.SerializationException;
import com.radixdlt.serialization.SerializerId2;

import java.util.UUID;

@SerializerId2("radix.particles.chess2.move")
public final class ChessMoveParticle extends Particle {
	@JsonProperty("gameAddress")
	@DsonOutput(DsonOutput.Output.ALL)
	private RadixAddress gameAddress;

	@JsonProperty("gameId")
	@DsonOutput(DsonOutput.Output.ALL)
	private String gameId;

	@JsonProperty("move")
	@DsonOutput(DsonOutput.Output.ALL)
	private String move;

	private ChessMoveParticle() {
		// for serializer
	}

	public ChessMoveParticle(String gameId, RadixAddress gameAddress, String move) {
		this.move = move;
		this.gameAddress = gameAddress;
		this.gameId = gameId;
	}

	public String getMove() {
		return move;
	}

	public RadixAddress getGameAddress() {
		return gameAddress;
	}

	public String getGameId() {
		return gameId;
	}

	@Override
	public String toString() {
		return "ChessMoveParticle{" +
			"move='" + move + '\'' +
			'}';
	}

	public static void main(String[] args) throws SerializationException {
		Serialization serialization = DefaultSerialization.getInstance();
		ChessMoveParticle particle = new ChessMoveParticle(
			UUID.randomUUID().toString(),
			RadixAddress.from("JH1P8f3znbyrDj8F4RWpix7hRkgxqHjdW2fNnKpR3v6ufXnknor"),
			"f7f6"
		);
		String json = serialization.toJson(particle, DsonOutput.Output.ALL);
		System.out.println(json);
	}
}
