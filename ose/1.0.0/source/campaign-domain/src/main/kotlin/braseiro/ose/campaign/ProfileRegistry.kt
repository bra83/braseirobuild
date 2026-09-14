package braseiro.ose.campaign

import braseiro.ose.model.RuleProfile

class MissingProfileProviderException(profile: RuleProfile) : IllegalStateException("No provider registered for exact profile $profile; silent fallback is forbidden")
class ProfileRegistry<T>(private val exactProviders: Map<RuleProfile, T>) {
    fun resolve(profile: RuleProfile): T = exactProviders[profile] ?: throw MissingProfileProviderException(profile)
}
