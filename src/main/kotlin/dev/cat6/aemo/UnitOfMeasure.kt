package dev.cat6.aemo

/**
 * [Appendix B] Allowed Values in the UOM field
 */
enum class UnitOfMeasure {
    MWH, // megawatt hour
    KWH, // kilowatt hour
    WH, // watt hour
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
}