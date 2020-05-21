package com.radixdlt.atommodel.chess;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.radixdlt.DefaultSerialization;
import com.radixdlt.constraintmachine.Particle;
import com.radixdlt.identifiers.RadixAddress;
import com.radixdlt.serialization.DsonOutput;
import com.radixdlt.serialization.Serialization;
import com.radixdlt.serialization.SerializationException;
import com.radixdlt.serialization.SerializerId2;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@SerializerId2("radix.particles.chess2.board")
public final class ChessBoardParticle extends Particle {
	@JsonProperty("boardFen")
	@DsonOutput(DsonOutput.Output.ALL)
	private String boardStateFen;

	private GameState gameState;

	@JsonProperty("gameAddress")
	@DsonOutput(DsonOutput.Output.ALL)
	private RadixAddress gameAddress;

	@JsonProperty("whiteAddress")
	@DsonOutput(DsonOutput.Output.ALL)
	private RadixAddress whiteAddress;

	@JsonProperty("blackAddress")
	@DsonOutput(DsonOutput.Output.ALL)
	private RadixAddress blackAddress;

	@JsonProperty("gameId")
	@DsonOutput(DsonOutput.Output.ALL)
	private String gameId;

	@JsonProperty("nonce")
	@DsonOutput(DsonOutput.Output.ALL)
	private long nonce;

	@JsonProperty("lastMoveWhite")
	@DsonOutput(DsonOutput.Output.ALL)
	private boolean lastMoveWhite;

	@JsonProperty("lastMove")
	@DsonOutput(DsonOutput.Output.ALL)
	private String lastMove;

	private ChessBoardParticle() {
		// for serializer
	}

	public ChessBoardParticle(
		String boardStateFen,
		GameState gameState,
		RadixAddress gameAddress,
		RadixAddress whiteAddress,
		RadixAddress blackAddress,
		String gameId,
		long nonce,
		boolean lastMoveWhite,
		String lastMove
	) {
		this.boardStateFen = boardStateFen;
		this.gameState = gameState;
		this.gameAddress = gameAddress;
		this.whiteAddress = whiteAddress;
		this.blackAddress = blackAddress;
		this.gameId = gameId;
		this.nonce = nonce;
		this.lastMoveWhite = lastMoveWhite;
		this.lastMove = lastMove;
	}

	public RadixAddress getGameAddress() {
		return gameAddress;
	}

	public RadixAddress getWhiteAddress() {
		return whiteAddress;
	}

	public RadixAddress getBlackAddress() {
		return blackAddress;
	}

	public String getGameId() {
		return gameId;
	}

	public String getBoardStateFen() {
		return boardStateFen;
	}

	public boolean isLastMoveWhite() {
		return lastMoveWhite;
	}

	public String getLastMove() {
		return lastMove;
	}

	public GameState getGameState() {
		return gameState;
	}

	@JsonProperty("gameState")
	@DsonOutput(value = {DsonOutput.Output.ALL})
	private String getJsonState() {
		return this.gameState.getName();
	}

	@JsonProperty("gameState")
	private void setJsonPermissions(String state) {
		this.gameState = GameState.from(state);
	}

	@Override
	public String toString() {
		return "ChessBoardParticle{" +
			"boardState='" + boardStateFen + '\'' +
			", gameAddress=" + gameAddress +
			", whiteAddress=" + whiteAddress +
			", blackAddress=" + blackAddress +
			", gameUID=" + gameId +
			", nonce=" + nonce +
			", lastMoveWhite=" + lastMoveWhite +
			'}';
	}

	enum GameState {
		INITIAL("initial"),
		ACTIVE("active"),
		WHITE_WON("whiteWon"),
		BLACK_WON("blackWon"),
		DRAW("draw");

		private final String name;

		GameState(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public boolean isFinal() {
			return this != ACTIVE && this != INITIAL;
		}

		public boolean isInitial() {
			return this == INITIAL;
		}

		public static GameState from(String name) {
			Map<String, GameState> statesByName = Arrays.stream(values())
				.collect(Collectors.toMap(state -> state.name, state -> state));

			if (!statesByName.containsKey(name)) {
				throw new IllegalStateException("Unknown game state '" + name + "', ");
			}

			return statesByName.get(name);
		}
	}

	public static void main(String[] args) throws SerializationException {
		Serialization serialization = DefaultSerialization.getInstance();
		ChessBoardParticle particle = new ChessBoardParticle(
			"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
			GameState.INITIAL,
			RadixAddress.from("JH1P8f3znbyrDj8F4RWpix7hRkgxqHjdW2fNnKpR3v6ufXnknor"),
			RadixAddress.from("JH1P8f3znbyrDj8F4RWpix7hRkgxqHjdW2fNnKpR3v6ufXnknor"),
			RadixAddress.from("JH1P8f3znbyrDj8F4RWpix7hRkgxqHjdW2fNnKpR3v6ufXnknor"),
			UUID.randomUUID().toString(),
			42,
			false,
			"f6f7"
		);
		String json = serialization.toJson(particle, DsonOutput.Output.ALL);
		System.out.println(json);
	}
}
