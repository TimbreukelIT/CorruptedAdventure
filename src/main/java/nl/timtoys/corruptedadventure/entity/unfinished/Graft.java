package nl.timtoys.corruptedadventure.entity.unfinished;

/**
 * The four pieces of the defeated Nightmares grafted onto The Unfinished. Each carries a weak spot and a memory attack;
 * once its weak spot has taken enough, the graft falls away and its memory with it (Voorlopig, R5 plan Q3).
 */
public enum Graft {
    /** The Hollow's giant arm: verspringen through the void. */
    HOLLE,
    /** The Layered's legs: grabbing and lifting a player. */
    GELAAGDE,
    /** The Risen's wing: ash rain. */
    HERRIJZENDE,
    /** The Lost Keeper's tail and head: the lure. */
    HOEDER;

    public int bit() {
        return 1 << ordinal();
    }
}
