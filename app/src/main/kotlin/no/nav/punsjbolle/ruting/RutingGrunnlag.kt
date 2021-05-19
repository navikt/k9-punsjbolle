package no.nav.punsjbolle.ruting

internal data class RutingGrunnlag(
    internal val søker: Boolean? = null,
    internal val pleietrengende: Boolean? = null,
    internal val annenPart: Boolean? = null) {
    init { require(søker != null || pleietrengende != null || annenPart != null) {"Minst et part må være evaluert."} }
    internal val minstEnPart = søker == true || pleietrengende == true || annenPart == true
}