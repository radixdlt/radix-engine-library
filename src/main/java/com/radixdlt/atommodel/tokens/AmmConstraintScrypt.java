package com.radixdlt.atommodel.tokens;

import com.radixdlt.atomos.ConstraintScrypt;
import com.radixdlt.atomos.Result;
import com.radixdlt.atomos.SysCalls;

public class AmmConstraintScrypt  implements ConstraintScrypt {

	@Override
	public void main(SysCalls os) {
		os.registerParticle(
			AmmParticle.class,
			particle -> particle.getRRI().getAddress(),
			t -> Result.success(),
			AmmParticle::getRRI
		);
	}
}
