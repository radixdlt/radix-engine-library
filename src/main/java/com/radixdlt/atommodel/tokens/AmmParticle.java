package com.radixdlt.atommodel.tokens;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.radixdlt.constraintmachine.Particle;
import com.radixdlt.identifiers.RRI;
import com.radixdlt.serialization.DsonOutput;
import com.radixdlt.serialization.SerializerId2;
import com.radixdlt.utils.UInt256;

@SerializerId2("radix.particles.amm")
public class AmmParticle extends Particle {
	@JsonProperty("rri")
	@DsonOutput(DsonOutput.Output.ALL)
	private RRI rri;

	@JsonProperty("tokenA")
	@DsonOutput(DsonOutput.Output.ALL)
	private RRI tokenA;

	@JsonProperty("tokenB")
	@DsonOutput(DsonOutput.Output.ALL)
	private RRI tokenB;

	@JsonProperty("aAmount")
	@DsonOutput(DsonOutput.Output.ALL)
	private UInt256 aAmount;

	@JsonProperty("bAmount")
	@DsonOutput(DsonOutput.Output.ALL)
	private UInt256 bAmount;

	public RRI getRRI() {
		return rri;
	}
}
