package org.mjdev.libs.barcodescanner.parsers.base

import android.annotation.SuppressLint
import java.util.*

/**
 * Standard bank QR Payment qr code parser CZ invoices
 */
@Suppress("MemberVisibilityCanBePrivate", "SpellCheckingInspection")
@SuppressLint("DefaultLocale")
abstract class QRCBAParsedResult(text: String) : QRPaymentParsedResult(text) {
    var id: String = ""
    var dd: String = ""
    var tp: String = ""
    var am: String = ""
    var msg: String = ""
    var vs: String = ""
    var vii: String = ""
    var ini: String = ""
    var inr: String = ""
    var vir: String = ""
    var duzp: String = ""
    var dt: String = ""
    var tb0: String = ""
    var tb1: String = ""
    var t0: String = ""
    var t1: String = ""
    var ntb: String = ""
    var cc: String = ""
    var td: String = ""
    var acc: String = ""
    var xsw: String = ""
    var xvs: String = ""
    var on: String = ""
    var bic: String = ""
    var iban: String = ""
    var nta:String = ""

    override fun parseData(rawData: String) {
        rawData.split("*").mapNotNull { row ->
            row.split(":").let { vp ->
                if (vp.size == 2) Pair(vp[0].lowercase(Locale.getDefault()), vp[1])
                else null
            }
        }.toMap().apply {
            id = get("id") ?: ""
            dd = get("dd") ?: ""
            tp = get("tp") ?: ""
            am = get("am") ?: ""
            msg = get("msg") ?: ""
            vs = get("vs") ?: ""
            vii = get("vii") ?: ""
            ini = get("ini") ?: ""
            inr = get("inr") ?: ""
            vir = get("vir") ?: ""
            duzp = get("duzp") ?: ""
            dt = get("dt") ?: ""
            tb0 = get("tb0") ?: ""
            tb1 = get("tb1") ?: ""
            t0 = get("t0") ?: ""
            t1 = get("t1") ?: ""
            ntb = get("ntb") ?: ""
            cc = get("cc") ?: ""
            td = get("td") ?: ""
            acc = get("acc") ?: ""
            xsw = get("x-sw") ?: ""
            xvs = get("x-vs") ?: ""
            on = get("on") ?: ""
            nta = get("nta") ?: ""
            if (vs.isEmpty()) vs = xvs
            bic = parseBIC(acc)
            iban = parseIBAN(acc)
            amount = am
            email = nta
            currency = Currency.getInstance(cc)
            invoiceId = on.ifEmpty {
                if (id.isEmpty()) vs
                else {
                    if (id.length > 20) id.substring(0, 20)
                    else id
                }
            }
        }
    }

    private fun parseBIC(acc: String): String {
        return if (acc.contains('+')) acc.substring(acc.indexOf('+') + 1)
        else ""
    }

    private fun parseIBAN(acc: String): String {
        return if (acc.contains('+')) acc.substring(0, acc.indexOf('+'))
        else ""
    }

    override fun getDisplayResult(): String {
        val result = StringBuilder(100)
        maybeAppend(id, result)
        maybeAppend(dd, result)
        maybeAppend(tp, result)
        maybeAppend(am, result)
        maybeAppend(msg, result)
        maybeAppend(vs, result)
        maybeAppend(vii, result)
        maybeAppend(ini, result)
        maybeAppend(inr, result)
        maybeAppend(vir, result)
        maybeAppend(duzp, result)
        maybeAppend(dt, result)
        maybeAppend(tb0, result)
        maybeAppend(tb1, result)
        maybeAppend(t0, result)
        maybeAppend(t1, result)
        maybeAppend(ntb, result)
        maybeAppend(cc, result)
        maybeAppend(td, result)
        maybeAppend(acc, result)
        maybeAppend(iban, result)
        maybeAppend(bic, result)
        maybeAppend(xsw, result)
        maybeAppend(xvs, result)
        maybeAppend(on, result)
        maybeAppend(nta, result)
        maybeAppend(amount, result)
        maybeAppend(currency?.currencyCode, result)
        maybeAppend(invoiceId, result)
        maybeAppend(email, result)
        maybeAppend(phone, result)
        return result.toString()
    }
}
