package dealdialect.engine;

/**
 * Role in negotiation
 */
public enum Role {
    BUYER,
    SELLER;
    
    public Role opposite() {
        return this == BUYER ? SELLER : BUYER;
    }
}

