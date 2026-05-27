package net.adbogm;

public enum QueryLanguage {
    SQL("sql"),
    OPENCYPHER("opencypher");

    private final String arcadeLanguage;

    QueryLanguage(String arcadeLanguage) {
        this.arcadeLanguage = arcadeLanguage;
    }

    @Override
    public String toString() {
        return arcadeLanguage;
    }
}
