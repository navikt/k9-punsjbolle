package no.nav.punsjbolle.ruting

data class RutingGrunnlag(
    internal val søker: Boolean,
    internal val pleietrengende: Boolean,
    internal val annenPart: Boolean) {
    internal val minstEnPart = søker || pleietrengende || annenPart
}