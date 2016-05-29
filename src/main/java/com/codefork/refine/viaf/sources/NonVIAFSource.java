package com.codefork.refine.viaf.sources;

import com.codefork.refine.SearchQuery;
import com.codefork.refine.StringUtil;
import com.codefork.refine.resources.Result;
import com.codefork.refine.viaf.VIAFResult;

import java.util.HashMap;
import java.util.Map;

/**
 * All non-VIAF sources.
 */
public class NonVIAFSource extends Source {

    private static final Map<String, String> urls = new HashMap<String, String>();

    static {
        urls.put("BNE", "http://catalogo.bne.es/uhtbin/authoritybrowse.cgi?action=display&authority_id={{id}}");
        // B2Q = no outgoing link
        // BAV = no outgoing link
        // BIBSYS = no outgoing link
        // BNC = no outgoing link
        // BNCHL = no outgoing link
        urls.put("BNF", "http://catalogue.bnf.fr/ark:/12148/cb{{id}}");
        // DBC = no outgoing link
        urls.put("DNB", "http://d-nb.info/gnd/{{id}}");
        // EGAXA = no outgoing link
        // ICCU = id in URL not same as parsed id (TODO?)
        urls.put("JPG", "http://www.getty.edu/vow/ULANFullDisplay?find=&role=&nation=&subjectid={{id}}");
        // KRNLK = no outgoing link
        // LAK = no outgoing link
        urls.put("LC", "http://id.loc.gov/authorities/names/{{id}}");
        // LNL = no outgoing link
        // MRBNR = no outgoing link
        urls.put("NDL", "http://id.ndl.go.jp/auth/ndlna/{{id}}");
        // N6I = no outgoing link
        // NKC = no outgoing link
        // NLA = no outgoing link
        // NLI = no outgoing link
        // NLP = no outgoing link
        // NLR = no outgoing link
        // NSK = no outgoing link
        // NTA = no outgoing link
        // NUKAT = no outgoing link
        // PTBNP = no outgoing link
        // RERO = no outgoing link
        urls.put("SELIBR", "http://libris.kb.se/resource/auth/{{id}}");
        urls.put("SUDOC", "http://www.idref.fr/{{id}}/id");
        // SWNL = no outgoing link
        urls.put("WKP", "http://www.wikidata.org/entity/{{id}}#sitelinks-wikipedia");
    }

    private String code;

    public NonVIAFSource(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String formatID(String id) {
        if(id != null) {
            if("LC".equals(code)) {
                return id.replace(" ", "");
            }
        }
        return id;
    }

    public String getServiceURLTemplate() {
        String url = urls.get(code);
        if(url == null) {
            url = String.format("https://viaf.org/viaf/sourceID/%s|{{id}}", code);
        }
        return url;
    }

    @Override
    public Result formatResult(SearchQuery query, VIAFResult viafResult) {
        // if no explicit source was specified, we should use any exact
        // match if present, otherwise the most common one
        String name = query.getSource() != null ?
                viafResult.getNameBySource(query.getSource()) :
                viafResult.getExactNameOrMostCommonName(query.getQuery());
        boolean exactMatch = name != null ? name.equals(query.getQuery()) : false;

        // if there's no source ID, we still have to return something,
        // so we return 0.
        String sourceId = viafResult.getSourceId(query.getSource());
        if(sourceId == null) {
            sourceId = "0";
        }

        Result r = new Result(
                formatID(sourceId),
                name,
                viafResult.getNameType(),
                StringUtil.levenshteinDistanceRatio(name, query.getQuery()),
                exactMatch);
        return r;
    }

}
