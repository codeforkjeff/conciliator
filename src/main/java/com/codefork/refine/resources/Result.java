
package com.codefork.refine.resources;

import com.codefork.refine.viaf.VIAFNameType;

import java.util.ArrayList;
import java.util.List;

/**
 * A single name result. The JSON looks like this:
 * 
 * {
 *     'id' : '...',
 *     'name': '...',
 *     'type': [
 *         {
 *             "id": '...',
 *             "name": '...',
 *         },
 *     ],
 *     'score': 1,
 *     'match': true
 * }
 */
public class Result {
    
    private String id;
    private String name;

    /**
     * It's weird that this is a list but that's what OpenRefine expects.
     * A VIAF result only ever has one type, but maybe other reconciliation
     * services return multiple types for a name?
     */
    private List<NameType> type;
    private double score;
    private boolean match;

    public Result(String id, String name, VIAFNameType nameType, double score, boolean match) {
        this.id = id;
        this.name = name;
        this.type = new ArrayList<NameType>();
        this.type.add(nameType.asNameType());
        this.score = score;
        this.match = match;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<NameType> getType() {
        return type;
    }

    public void setType(List<NameType> resultType) {
        this.type = resultType;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public boolean isMatch() {
        return match;
    }

    public void setMatch(boolean match) {
        this.match = match;
    }

}
