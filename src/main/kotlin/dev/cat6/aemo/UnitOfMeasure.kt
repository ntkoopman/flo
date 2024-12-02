package dev.cat6.aemo

import java.math.BigDecimal

/**
 * Allowed Values in the UOM field
 *
 * See Appendix B
 */
enum class UnitOfMeasure(
    // Conversion factor for converting to kWh
    private val conversion: BigDecimal? = null
) {
  MWH(BigDecimal("0.001")), // megawatt hour
  KWH(BigDecimal.ONE), // kilowatt hour
  WH(BigDecimal("1000")), // watt hour
  MVARH, // megavolt ampere reactive hour (megavar hour)
  KVARH, // kilovolt ampere reactive hour
  VARH, // volt ampere reactive hour
  MVAR, // megavolt ampere reactive
  KVAR, // kilovolt ampere reactive
  VAR, // volt ampere reactive
  MW, // megawatt
  KW, // kilowatt
  W, // watt
  MVAH, // megavolt ampere hour
  KVAH, // kilovolt ampere hour
  VAH, // volt ampere hour
  MVA, // megavolt ampere
  KVA, // kilovolt ampere
  VA, // volt ampere
  KV, // kilovolt
  V, // volt
  KA, // kiloampere
  A, // ampere
  PF, // Power Factor
  ;

  fun toKWH(usage: BigDecimal): BigDecimal {
    if (conversion == null) throw RuntimeException("Unsupported conversion: $this to kWh")
    return usage.multiply(conversion)
  }
}
