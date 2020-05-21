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
			return String.valueOf(this.amount);
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
	}
}
