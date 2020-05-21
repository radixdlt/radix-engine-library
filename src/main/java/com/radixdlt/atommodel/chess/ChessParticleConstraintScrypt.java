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

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;
import com.google.common.reflect.TypeToken;
import com.radixdlt.atomos.ConstraintScrypt;
import com.radixdlt.atomos.ParticleDefinition;
import com.radixdlt.atomos.Result;
import com.radixdlt.atomos.SysCalls;
import com.radixdlt.constraintmachine.Spin;
import com.radixdlt.constraintmachine.TransitionProcedure;
import com.radixdlt.constraintmachine.TransitionToken;
import com.radixdlt.constraintmachine.UsedCompute;
import com.radixdlt.constraintmachine.VoidUsedData;
import com.radixdlt.constraintmachine.WitnessValidator;

import java.util.Optional;

public class ChessParticleConstraintScrypt implements ConstraintScrypt {
	@Override
	public void main(SysCalls os) {
		os.registerParticle(ChessBoardParticle.class, ParticleDefinition.<ChessBoardParticle>builder()
			.singleAddressMapper(ChessBoardParticle::getGameAddress)
			.virtualizeSpin(board -> board.getGameState() == ChessBoardParticle.GameState.INITIAL ? Spin.UP : null)
			.staticValidation(ChessParticleConstraintScrypt::staticCheck)
			.build());

		os.createTransition(
			new TransitionToken<>(ChessBoardParticle.class, TypeToken.of(VoidUsedData.class), ChessBoardParticle.class, TypeToken.of(VoidUsedData.class)),
			new TransitionProcedure<ChessBoardParticle, VoidUsedData, ChessBoardParticle, VoidUsedData>() {
				@Override
				public Result precondition(ChessBoardParticle inputBoard, VoidUsedData inputUsed, ChessBoardParticle outputBoard, VoidUsedData outputUsed) {
					return Result.combine(
						checkSameGame(inputBoard, outputBoard),
						checkTurn(inputBoard, outputBoard),
						checkMoveAllowed(inputBoard, outputBoard)
					);
				}

				@Override
				public UsedCompute<ChessBoardParticle, VoidUsedData, ChessBoardParticle, VoidUsedData> inputUsedCompute() {
					return (input, inputUsed, output, outputUsed) -> Optional.empty();
				}

				@Override
				public UsedCompute<ChessBoardParticle, VoidUsedData, ChessBoardParticle, VoidUsedData> outputUsedCompute() {
					return (input, inputUsed, output, outputUsed) -> Optional.empty();
				}

				@Override
				public WitnessValidator<ChessBoardParticle> inputWitnessValidator() {
					// TODO check if signed
					return (i, w) -> WitnessValidator.WitnessValidatorResult.success();
				}

				@Override
				public WitnessValidator<ChessBoardParticle> outputWitnessValidator() {
					// TODO check if signed
					return (o, w) -> WitnessValidator.WitnessValidatorResult.success();
				}
			}
		);

	}

	private static Result checkSameGame(ChessBoardParticle input, ChessBoardParticle output) {
		return Result.combine(
			Result.of(input.getGameAddress().equals(output.getGameAddress()),
				"boards have different addresses: " + input.getGameAddress() + " vs " + output.getGameAddress()),
			Result.of(input.getGameId().equals(output.getGameId()),
				"boards have different ids: " + input.getGameId() + " vs " + output.getGameId()),
			Result.of(input.getWhiteAddress().equals(output.getWhiteAddress()),
				"boards have different white address: " + input.getWhiteAddress() + " vs " + output.getWhiteAddress()),
			Result.of(input.getBlackAddress().equals(output.getBlackAddress()),
				"boards have different black address: " + input.getBlackAddress() + " vs " + output.getBlackAddress())
		);
	}

	private static Result checkTurn(ChessBoardParticle input, ChessBoardParticle output) {
		Side nextSide = input.isLastMoveWhite() ? Side.BLACK : Side.WHITE;
		Side nextNextSide = output.isLastMoveWhite() ? Side.BLACK : Side.WHITE;
		return Result.combine(
			Result.of(!input.getGameState().isFinal(), "game already ended: " + input.getGameState()),
			Result.of(input.getGameState().isInitial() || nextSide != nextNextSide, String.format("side %s cannot move twice", nextSide))
		);
	}

	private static Result checkMoveAllowed(ChessBoardParticle input, ChessBoardParticle output) {
		try {
			Side nextSide = input.isLastMoveWhite() ? Side.BLACK : Side.WHITE;
			Move nextMove = new Move(output.getLastMove(), nextSide);
			final Board prevBoard = new Board();
			prevBoard.loadFromFen(input.getBoardStateFen());
			prevBoard.setSideToMove(nextSide);

			if (!prevBoard.doMove(nextMove, true)) {
				return Result.error("move " + output.getLastMove() + " is not allowed on '" + input.getBoardStateFen() + "'");
			}

			final Board nextBoard = new Board();
			nextBoard.loadFromFen(output.getBoardStateFen());
			if (!prevBoard.equals(nextBoard)) {
				return Result.error(String.format("input board with move does not equal next board: '%s' + '%s' != '%s'",
					prevBoard.getFen(), output.getLastMove(), nextBoard.getFen()));
			}

			ChessBoardParticle.GameState nextState = ChessBoardParticle.GameState.ACTIVE;
			if (nextBoard.isDraw()) {
				nextState = ChessBoardParticle.GameState.DRAW;
			} else if (nextBoard.isMated()) {
				nextState = nextSide == Side.WHITE ? ChessBoardParticle.GameState.WHITE_WON : ChessBoardParticle.GameState.BLACK_WON;
			}

			if (output.getGameState() != nextState) {
				return Result.error("Next game state does not equal claimed next state: " + nextState + " != " + output.getGameState());
			}
		} catch (Exception e) {
			return Result.error(String.format("unable to verify move '%s' from '%s' to '%s': %s",
				output.getLastMove(), input.getBoardStateFen(), output.getBoardStateFen(), e.getMessage()));
		}

		return Result.success();
	}

	private static Result checkBoardFormat(String boardStateFen) {
		try {
			Board parsedBoard = new Board();
			parsedBoard.loadFromFen(boardStateFen);

			if (!parsedBoard.getFen().equals(boardStateFen)) {
				return Result.error(String.format("parsed board state does not equal given: '%s' != '%s'", parsedBoard.getFen(), boardStateFen));
			}
		} catch (Exception e) {
			return Result.error(String.format("could not parse board '%s': %s", boardStateFen, e.getMessage()));
		}

		return Result.success();
	}

	private static Result staticCheck(ChessBoardParticle board) {
		// TODO static check other board params (e.g. not null)
		return checkBoardFormat(board.getBoardStateFen());
	}
}
