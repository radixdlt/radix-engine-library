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

	public static final class SwapUsedAmount implements UsedData {
		private final RRI rriSend;
		private final UInt256 swapSent;
		private final RRI rriReceive;
		private final UInt256 swapReceive;

		public SwapUsedAmount(RRI rriSend, UInt256 swapSent, RRI rriReceive, UInt256 swapReceive) {
			this.rriSend = rriSend;
			this.rriReceive = rriReceive;
			this.swapSent = swapSent;
			this.swapReceive = swapReceive;
		}

		@Override
		public TypeToken<? extends UsedData> getTypeToken() {
			return TypeToken.of(SwapUsedAmount.class);
		}

		@Override
		public int hashCode() {
			return Objects.hash(swapReceive, swapSent, rriReceive, rriSend);
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof SwapUsedAmount)) {
				return false;
			}

			SwapUsedAmount u = (SwapUsedAmount) obj;
			return Objects.equals(this.swapReceive, u.swapReceive)
				&& Objects.equals(this.swapSent, u.swapSent)
				&& Objects.equals(this.rriReceive, u.rriReceive)
				&& Objects.equals(this.rriSend, u.rriSend);
		}

		@Override
		public String toString() {
			return "sent " + swapSent + " " + rriSend + " receive " + swapReceive + " " + rriReceive;
		}
	}


	public static final class SwapSentAmount implements UsedData {
		private final RRI rriSent;
		private final UInt256 swapSent;
		private final UInt256 swapRequired;

		public SwapSentAmount(RRI rriSent, UInt256 swapSent, UInt256 swapRequired) {
			this.rriSent = rriSent;
			this.swapSent = swapSent;
			this.swapRequired = swapRequired;
		}

		@Override
		public TypeToken<? extends UsedData> getTypeToken() {
			return TypeToken.of(SwapUsedAmount.class);
		}

		@Override
		public int hashCode() {
			return Objects.hash(rriSent, swapSent, swapRequired);
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof SwapSentAmount)) {
				return false;
			}

			SwapSentAmount u = (SwapSentAmount) obj;
			return Objects.equals(this.rriSent, u.rriSent)
				&& Objects.equals(this.swapSent, u.swapSent)
				&& Objects.equals(this.swapRequired, u.swapRequired);
		}

		@Override
		public String toString() {
			return " sent " + rriSent + " " + swapSent + " " + swapRequired;
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
					return (input, inputUsed, output, outputUsed) -> {
						// Swap: A -> B
						if (output.getaAmount().compareTo(input.getaAmount()) >= 0) {
							return Optional.of(new SwapUsedAmount(
								output.getTokenA(),
								output.getaAmount().subtract(input.getaAmount()),
								output.getTokenB(),
								input.getbAmount().subtract(output.getbAmount())
							));
						} else {
							return Optional.of(new SwapUsedAmount(
								output.getTokenB(),
								input.getaAmount().subtract(output.getaAmount()),
								output.getTokenA(),
								output.getbAmount().subtract(input.getbAmount())
							));
						}
					};
				}

				@Override
				public UsedCompute<AmmParticle, VoidUsedData, AmmParticle, VoidUsedData> outputUsedCompute() {
					return (input, inputUsed, output, outputUsed) -> Optional.empty();
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
			new TransitionToken<>(AmmParticle.class, TypeToken.of(SwapUsedAmount.class), TransferrableTokensParticle.class, TypeToken.of(VoidUsedData.class)),
			new TransitionProcedure<AmmParticle, SwapUsedAmount, TransferrableTokensParticle, VoidUsedData>() {
				@Override
				public Result precondition(AmmParticle inputParticle, SwapUsedAmount inputUsed, TransferrableTokensParticle outputParticle, VoidUsedData outputUsed) {
					return outputParticle.getTokDefRef().equals(inputUsed.rriReceive)
						? Result.success() : Result.error("Wrong token Type");
				}

				@Override
				public UsedCompute<AmmParticle, SwapUsedAmount, TransferrableTokensParticle, VoidUsedData> inputUsedCompute() {
					return (input, inputUsed, output, outputUsed) -> {
						if (inputUsed.swapReceive.compareTo(output.getAmount()) >= 0) {
							return Optional.empty();
						} else {
							return Optional.of(new SwapUsedAmount(inputUsed.rriSend, inputUsed.swapSent, inputUsed.rriReceive,
								inputUsed.swapReceive.subtract(output.getAmount())));
						}
					};
				}

				@Override
				public UsedCompute<AmmParticle, SwapUsedAmount, TransferrableTokensParticle, VoidUsedData> outputUsedCompute() {
					return (input, inputUsed, output, outputUsed) -> {
						if (inputUsed.swapReceive.compareTo(output.getAmount()) >= 0) {
							return Optional.of(new SwapSentAmount(inputUsed.rriSend, UInt256.ZERO, inputUsed.swapSent));
						} else {
							return Optional.of(new SwapUsedAmount(inputUsed.rriSend, inputUsed.swapSent, inputUsed.rriReceive,
								inputUsed.swapReceive.subtract(output.getAmount())));
						}
					};
				}

				@Override
				public WitnessValidator<AmmParticle> inputWitnessValidator() {
					return (o, witnessData) -> WitnessValidatorResult.success();
				}

				@Override
				public WitnessValidator<TransferrableTokensParticle> outputWitnessValidator() {
					return (o, witnessData) -> WitnessValidatorResult.success();
				}
			}
		);

		os.createTransition(
			new TransitionToken<>(TransferrableTokensParticle.class, TypeToken.of(VoidUsedData.class), TransferrableTokensParticle.class, TypeToken.of(SwapSentAmount.class)),
			new TransitionProcedure<TransferrableTokensParticle, VoidUsedData, TransferrableTokensParticle, SwapSentAmount>() {
				@Override
				public Result precondition(TransferrableTokensParticle inputParticle, VoidUsedData inputUsed, TransferrableTokensParticle outputParticle, SwapSentAmount outputUsed) {
					return inputParticle.getTokDefRef().equals(outputUsed.rriSent)
						? Result.success() : Result.error("Wrong token Type");
				}

				@Override
				public UsedCompute<TransferrableTokensParticle, VoidUsedData, TransferrableTokensParticle, SwapSentAmount> inputUsedCompute() {
					return (input, inputUsed, output, outputUsed) -> {
						UInt256 totalInput = input.getAmount().add(outputUsed.swapSent);
						if (totalInput.compareTo(outputUsed.swapRequired) <= 0) {
							return Optional.empty();
						} else {
							return Optional.of(new CreateFungibleTransitionRoutine.UsedAmount(totalInput.subtract(outputUsed.swapRequired)));
						}
					};
				}

				@Override
				public UsedCompute<TransferrableTokensParticle, VoidUsedData, TransferrableTokensParticle, SwapSentAmount> outputUsedCompute() {
					return (input, inputUsed, output, outputUsed) -> {
						UInt256 totalInput = input.getAmount().add(outputUsed.swapSent);
						if (totalInput.compareTo(outputUsed.swapRequired) >= 0) {
							return Optional.empty();
						} else {
							return Optional.of(new SwapSentAmount(outputUsed.rriSent, totalInput, outputUsed.swapRequired));
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
