package structures;


public enum EntailmentLabel
{
    ENTAILMENT, CONTRADICTION, NEUTRAL, UNKNOWN;

    /**Due to UNKNOWN being "-", this is largely-but-not-quite
     * a replacement for .valueOf()
     *
     * @param s
     * @return
     */
    public static EntailmentLabel parseLabel(String s)
    {
        switch(s.toUpperCase()){
            case "ENTAILMENT": return ENTAILMENT;
            case "CONTRADICTION": return CONTRADICTION;
            case "NEUTRAL": return NEUTRAL;
            default: return UNKNOWN;
        }
    }
}
