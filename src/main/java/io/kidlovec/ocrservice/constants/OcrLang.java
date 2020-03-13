package io.kidlovec.ocrservice.constants;

import java.util.HashMap;
import java.util.Map;

/**
 * @author kidlovec
 * @date 2020-03-13
 * @since 1.0.0
 */
public enum OcrLang {
    afr("afr"),
    ara("ara"),
    aze("aze"),
    bel("bel"),
    ben("ben"),
    bul("bul"),
    cat("cat"),
    ces("ces"),
    chi_sim("chi_sim"),
    chi_tra("chi_tra"),
    chr("chr"),
    dan("dan"),
    deu("deu"),
    ell("ell"),
    eng("eng"),
    enm("enm"),
    epo("epo"),
    est("est"),
    eus("eus"),
    fin("fin"),
    fra("fra"),
    frk("frk"),
    frm("frm"),
    glg("glg"),
    grc("grc"),
    heb("heb"),
    hin("hin"),
    hrv("hrv"),
    hun("hun"),
    ind("ind"),
    isl("isl"),
    ita("ita"),
    ita_old("ita_old"),
    jpn("jpn"),
    kan("kan"),
    kor("kor"),
    lav("lav"),
    lit("lit"),
    mal("mal"),
    mkd("mkd"),
    mlt("mlt"),
    msa("msa"),
    nld("nld"),
    nor("nor"),
    pol("pol"),
    por("por"),
    ron("ron"),
    rus("rus"),
    slk("slk"),
    slv("slv"),
    spa("spa"),
    spa_old("spa_old"),
    sqi("sqi"),
    srp("srp"),
    swa("swa"),
    swe("swe"),
    tam("tam"),
    tel("tel"),
    tgl("tgl"),
    tha("tha"),
    tur("tur"),
    ukr("ukr"),
    vie("vie"),
    ;

    private String desc;

    OcrLang(String desc){
        this.desc = desc;
    }

    private static Map<String, OcrLang> MAP = new HashMap<>((int)Math.ceil(OcrLang.values().length / 0.7));

    static {
        for (OcrLang lang : OcrLang.values()) {
            MAP.put(lang.desc, lang);
        }
    }

    public static boolean getLangType(String lang){
        return MAP.containsKey(lang);
    }
}
