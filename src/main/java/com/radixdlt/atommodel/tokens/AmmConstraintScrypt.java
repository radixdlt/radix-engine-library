package com.radixdlt.atommodel.tokens;

import com.google.common.reflect.TypeToken;
import com.radixdlt.atommodel.routines.CreateFungibleTransitionRoutine;
import com.radixdlt.atomos.ConstraintScrypt;
import com.radixdlt.atomos.RRIParticle;
import com.radixdlt.atomos.Result;
import com.radixdlt.atomos.SysCalls;
import com.radixdlt.constraintmachine.TransitionProcedure;
import com.radixdlt.constraintmachine.TransitionToken;
import com.radixdlt.constraintmachine.UsedCompute;
import com.radixdlt.constraintmachine.UsedData;
import com.radixdlt.constraintmachine.VoidUsedData;
import com.radixdlt.constraintmachine.WitnessValidator;
import com.radixdlt.constraintmachine.WitnessValidator.WitnessValidatorResult;
import com.radixdlt.identifiers.RRI;
import com.radixdlt.utils.UInt256;
import java.util.Objects;
import java.util.Optional;

public class AmmConstraintScrypt  implements ConstraintScrypt {

	public static final class UsedAmount implements UsedData {
		private final UInt256 amount;
		private final boolean isA;

		public UsedAmount(UInt256 usedAmount, boolean isA) {
			this.amount = Objects.requireNonNull(usedAmount);
			this.isA = isA;
		}

		public UInt256 getUsedAmount() {
			return this.amount;
		}

		@Override
		public TypeToken<? extends UsedData> getTypeToken() {
			return TypeToken.of(UsedAmount.class);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(amount);
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof UsedAmount)) {
				return false;
			}

			UsedAmount u = (UsedAmount) obj;
			return Objects.equals(this.amount, u.amount);
		}

		@Override
		public String toString() {
			return (isA ? "A: " : "B: ") + String.valueOf(this.amount);
		}
	}

	public static final class SwapSentReceivedUsedAmount implements UsedData {
		private final SwapUsedAmount send;
		private final SwapUsedAmount receive;

		public SwapSentReceivedUsedAmount(SwapUsedAmount send, SwapUsedAmount receive) {
			this.send = send;
			this.receive = receive;
		}

		@Override
		public TypeToken<? extends UsedData> getTypeToken() {
			return TypeToken.of(SwapSentReceivedUsedAmount.class);
		}

		@Override
		public int hashCode() {
			return Objects.hash(send, receive);
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof SwapSentReceivedUsedAmount)) {
				return false;
			}

			SwapSentReceivedUsedAmount u = (SwapSentReceivedUsedAmount) obj;
			return Objects.equals(this.send, u.send)
				&& Objects.equals(this.receive, u.receive);
		}

		@Override
		public String toString() {
			return "send " + send + " receive " + receive;
		}
	}


	public static final class SwapUsedAmount implements UsedData {
		private final RRI rri;
		private final UInt256 swapUsed;
		private final UInt256 swapRequired;

		public SwapUsedAmount(RRI rri, UInt256 swapUsed, UInt256 swapRequired) {
			this.rri = rri;
			this.swapUsed = swapUsed;
			this.swapRequired = swapRequired;
		}

		@Override
		public TypeToken<? extends UsedData> getTypeToken() {
			return TypeToken.of(SwapSentReceivedUsedAmount.class);
		}

		@Override
		public int hashCode() {
			return Objects.hash(rri, swapUsed, swapRequired);
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof SwapUsedAmount)) {
				return false;
			}

			SwapUsedAmount u = (SwapUsedAmount) obj;
			return Objects.equals(this.rri, u.rri)
				&& Objects.equals(this.swapUsed, u.swapUsed)
				&& Objects.equals(this.swapRequired, u.swapRequired);
		}

		@Override
		public String toString() {
			return " sent " + rri + " " + swapUsed + " " + swapRequired;
		}
	}

	@Override
	public void main(SysCalls os) {
		os.registerParticle(
			AmmParticle.class,
			particle -> particle.getRRI().getAddress(),
			t -> Result.success(),
			AmmParticle::getRRI
		);

		os.createTransition(
			new TransitionToken<>(RRIParticle.class, TypeToken.of(VoidUsedData.class), AmmParticle.class, TypeToken.of(VoidUsedData.class)),
			new TransitionProcedure<RRIParticle, VoidUsedData, AmmParticle, VoidUsedData>() {
				@Override
				public Result precondition(RRIParticle inputParticle, VoidUsedData inputUsed, AmmParticle outputParticle, VoidUsedData outputUsed) {
					return Result.success();
				}

				@Override
				public UsedCompute<RRIParticle, VoidUsedData, AmmParticle, VoidUsedData> inputUsedCompute() {
					return (input, inputUsed, output, outputUsed) -> Optional.empty();
				}

				@Override
				public UsedCompute<RRIParticle, VoidUsedData, AmmParticle, VoidUsedData> outputUsedCompute() {
					return (input, inputUsed, output, outputUsed) -> Optional.of(new UsedAmount(UInt256.ZERO, true));
				}


				@Override
				public WitnessValidator<RRIParticle> inputWitnessValidator() {
					return (rri, witnessData) -> witnessData.isSignedBy(rri.getRri().getAddress().getPublicKey())
						? WitnessValidatorResult.success() : WitnessValidatorResult.error("Not signed by " + rri.getRri().getAddress());
				}

				@Override
				public WitnessValidator<AmmParticle> outputWitnessValidator() {
					return (o, witnessData) -> WitnessValidatorResult.success();
				}
			}
		);

		os.createTransition(
			new TransitionToken<>(TransferrableTokensParticle.class, TypeToken.of(VoidUsedData.class), AmmParticle.class, TypeToken.of(UsedAmount.class)),
			new TransitionProcedure<TransferrableTokensParticle, VoidUsedData, AmmParticle, UsedAmount>() {
				@Override
				public Result precondition(TransferrableTokensParticle inputParticle, VoidUsedData inputUsed, AmmParticle outputParticle, UsedAmount outputUsed) {
					return (outputUsed.isA && inputParticle.getTokDefRef().equals(outputParticle.getTokenA()))
						||  (!outputUsed.isA && inputParticle.getTokDefRef().equals(outputParticle.getTokenB()))
						? Result.success() : Result.error("Wrong token Type");
				}

				@Override
				public UsedCompute<TransferrableTokensParticle, VoidUsedData, AmmParticle, UsedAmount> inputUsedCompute() {
					return (input, inputUsed, output, outputUsed) -> Optional.empty();
				}

				@Override
				public UsedCompute<TransferrableTokensParticle, VoidUsedData, AmmParticle, UsedAmount> outputUsedCompute() {
					return (input, inputUsed, output, outputUsed) -> {
						UInt256 totalInput = input.getAmount().add(outputUsed.getUsedAmount());
						int compare = totalInput.compareTo(outputUsed.isA ? output.getaAmount() : output.getbAmount());
						if (compare >= 0) {
							if (outputUsed.isA) {
								return Optional.of(new UsedAmount(UInt256.ZERO, false));
							} else {
								return Optional.empty();
							}
						} else {
							return Optional.of(new UsedAmount(totalInput, true));
						}
					};
				}

				@Override
				public WitnessValidator<TransferrableTokensParticle> inputWitnessValidator() {
					return (input, witnessData) -> witnessData.isSignedBy(input.getAddress().getPublicKey())
						? WitnessValidatorResult.success() : WitnessValidatorResult.error("Not signed by " + input.getAddress());
				}

				@Override
				public WitnessValidator<AmmParticle> outputWitnessValidator() {
					return (o, witnessData) -> WitnessValidatorResult.success();
				}
			}
		);

		os.createTransition(
			new TransitionToken<>(AmmParticle.class, TypeToken.of(VoidUsedData.class), AmmParticle.class, TypeToken.of(VoidUsedData.class)),
			new TransitionProcedure<AmmParticle, VoidUsedData, AmmParticle, VoidUsedData>() {
				@Override
				public Result precondition(AmmParticle inputParticle, VoidUsedData inputUsed, AmmParticle outputParticle, VoidUsedData outputUsed) {
					UInt256 invariant = inputParticle.getaAmount().multiply(inputParticle.getbAmount());
					if (!invariant.divide(outputParticle.getbAmount()).equals(outputParticle.getaAmount())) {
						return Result.error("Invariant broken: expected " + invariant.divide(outputParticle.getbAmount()) + " but was "
							 + outputParticle.getaAmount());
					}

					return inputParticle.getRRI().equals(outputParticle.getRRI())
						? Result.success() : Result.error("Wrong token Type");
				}

				@Override
				public UsedCompute<AmmParticle, VoidUsedData, AmmParticle, VoidUsedData> inputUsedCompute() {
					return (input, inputUsed, output, outputUsed) -> Optional.empty();
				}

				@Override
				public UsedCompute<AmmParticle, VoidUsedData, AmmParticle, VoidUsedData> outputUsedCompute() {
					return (input, inputUsed, output, outputUsed) -> {
						final SwapUsedAmount send;
						final SwapUsedAmount receive;


						if (output.getaAmount().compareTo(input.getaAmount()) >= 0) { // Swap: A -> B
							send = new SwapUsedAmount(output.getTokenA(), UInt256.ZERO, output.getaAmount().subtract(input.getaAmount()));
							receive = new SwapUsedAmount(output.getTokenB(), UInt256.ZERO, input.getbAmount().subtract(output.getbAmount()));

						} else {
							send = new SwapUsedAmount(output.getTokenB(), UInt256.ZERO, output.getbAmount().subtract(input.getbAmount()));
							receive = new SwapUsedAmount(output.getTokenA(), UInt256.ZERO, input.getaAmount().subtract(output.getaAmount()));
						}

						return Optional.of(new SwapSentReceivedUsedAmount(
							send, receive
						));
					};
				}

				@Override
				public WitnessValidator<AmmParticle> inputWitnessValidator() {
					return (o, witnessData) -> WitnessValidatorResult.success();
				}

				@Override
				public WitnessValidator<AmmParticle> outputWitnessValidator() {
					return (o, witnessData) -> WitnessValidatorResult.success();
				}
			}
		);

		os.createTransition(
			new TransitionToken<>(TransferrableTokensParticle.class, TypeToken.of(VoidUsedData.class), AmmParticle.class, TypeToken.of(SwapSentReceivedUsedAmount.class)),
			new TransitionProcedure<TransferrableTokensParticle, VoidUsedData, AmmParticle, SwapSentReceivedUsedAmount>() {
				@Override
				public Result precondition(TransferrableTokensParticle inputParticle, VoidUsedData inputUsed, AmmParticle outputParticle, SwapSentReceivedUsedAmount outputUsed) {
					return inputParticle.getTokDefRef().equals(outputUsed.send.rri)
						? Result.success() : Result.error("Wrong token Type");
				}

				@Override
				public UsedCompute<TransferrableTokensParticle, VoidUsedData, AmmParticle, SwapSentReceivedUsedAmount> inputUsedCompute() {
					return (input, inputUsed, output, outputUsed) -> {
						UInt256 totalInput = outputUsed.send.swapUsed.add(input.getAmount());
						if (totalInput.compareTo(outputUsed.send.swapRequired) >= 0) {
							return Optional.of(outputUsed.receive);
						} else {
							return Optional.empty();
						}
					};
				}

				@Override
				public UsedCompute<TransferrableTokensParticle, VoidUsedData, AmmParticle, SwapSentReceivedUsedAmount> outputUsedCompute() {
					return (input, inputUsed, output, outputUsed) -> {
						UInt256 totalInput = outputUsed.send.swapUsed.add(input.getAmount());
						if (totalInput.compareTo(outputUsed.send.swapRequired) >= 0) {
							return Optional.empty();
						} else {
							return Optional.of(
								new SwapSentReceivedUsedAmount(
									new SwapUsedAmount(outputUsed.send.rri, totalInput, outputUsed.send.swapRequired),
									outputUsed.receive
								)
							);
						}
					};
				}

				@Override
				public WitnessValidator<TransferrableTokensParticle> inputWitnessValidator() {
					return (input, witnessData) -> witnessData.isSignedBy(input.getAddress().getPublicKey())
						? WitnessValidatorResult.success() : WitnessValidatorResult.error("Not signed by " + input.getAddress());
				}

				@Override
				public WitnessValidator<AmmParticle> outputWitnessValidator() {
					return (o, witnessData) -> WitnessValidatorResult.success();
				}
			}
		);

		os.createTransition(
			new TransitionToken<>(TransferrableTokensParticle.class, TypeToken.of(SwapUsedAmount.class), TransferrableTokensParticle.class, TypeToken.of(VoidUsedData.class)),
			new TransitionProcedure<TransferrableTokensParticle, SwapUsedAmount, TransferrableTokensParticle, VoidUsedData>() {
				@Override
				public Result precondition(TransferrableTokensParticle inputParticle, SwapUsedAmount inputUsed, TransferrableTokensParticle outputParticle, VoidUsedData outputUsed) {
					return outputParticle.getTokDefRef().equals(inputUsed.rri)
						? Result.success() : Result.error("Wrong token Type");
				}

				@Override
				public UsedCompute<TransferrableTokensParticle, SwapUsedAmount, TransferrableTokensParticle, VoidUsedData> inputUsedCompute() {
					return (input, inputUsed, output, outputUsed) -> {
						UInt256 totalInput = inputUsed.swapUsed.add(output.getAmount());
						if (totalInput.compareTo(inputUsed.swapRequired) <= 0) {
							return Optional.of(new SwapUsedAmount(inputUsed.rri, totalInput, inputUsed.swapRequired));
						} else {
							return Optional.empty();
						}
					};
				}

				@Override
				public UsedCompute<TransferrableTokensParticle, SwapUsedAmount, TransferrableTokensParticle, VoidUsedData> outputUsedCompute() {
					return (input, inputUsed, output, outputUsed) -> {
						UInt256 totalInput = inputUsed.swapUsed.add(output.getAmount());
						if (totalInput.compareTo(inputUsed.swapRequired) <= 0) {
							return Optional.empty();
						} else {
							// Taking out more than should
							return Optional.of(new SwapUsedAmount(inputUsed.rri, totalInput, inputUsed.swapRequired));
						}
					};
				}

				@Override
				public WitnessValidator<TransferrableTokensParticle> inputWitnessValidator() {
					return (input, witnessData) -> witnessData.isSignedBy(input.getAddress().getPublicKey())
						? WitnessValidatorResult.success() : WitnessValidatorResult.error("Not signed by " + input.getAddress());
				}

				@Override
				public WitnessValidator<TransferrableTokensParticle> outputWitnessValidator() {
					return (o, witnessData) -> WitnessValidatorResult.success();
				}
			}
		);
	}
}
