package com.example.practice;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOUtil;

/** Issues tokens that stand in for card numbers, and turns them back for the station. */
public final class TokenVault {

    /** Encryption and a keyed lookup value for card numbers. The vault never sees the keys. */
    public interface Sealer {
        String seal(String pan);

        String open(String sealed);

        String lookup(String pan);
    }

    record Entry(String requestor, String sealedPan) {
    }

    private final String tokenBin;
    private final Random random;
    private final Sealer sealer;
    private final Map<String, Entry> byToken = new ConcurrentHashMap<>();
    private final Map<String, String> byRequestorAndCard = new ConcurrentHashMap<>();

    /** tokenBin: the range the network reserved for this vault's tokens, 6 to 8 digits. */
    public TokenVault(String tokenBin, Random random, Sealer sealer) {
        this.tokenBin = tokenBin;
        this.random = random;
        this.sealer = sealer;
    }

    /** The requestor's token for this card: the one it already has, or a new one. */
    public String tokenize(String requestor, String pan) {
        return byRequestorAndCard.computeIfAbsent(key(requestor, pan),
            k -> issue(new Entry(requestor, sealer.seal(pan))));
    }

    /** The card number behind a token. The next practice makes this safe. */
    public String detokenize(Set<String> callerPermissions, String requestor, String token) throws ISOException {
        return sealer.open(byToken.get(token).sealedPan());
    }

    /** What the vault files a requestor's token for this card under. */
    String key(String requestor, String pan) {
        return requestor + ":" + pan;
    }

    /** A token nobody holds yet, recorded for this entry. */
    String issue(Entry entry) {
        String token = newToken();
        while (byToken.putIfAbsent(token, entry) != null) {
            token = newToken();
        }
        return token;
    }

    /** A candidate token: 16 digits, each drawn with random.nextInt(10) after the range. */
    String newToken() {
        StringBuilder sb = new StringBuilder(tokenBin);
        while (sb.length() < 15) {
            sb.append(random.nextInt(10));
        }
        return sb.append(ISOUtil.calcLUHN(sb.toString())).toString();
    }
}
