package com.example.practice;

import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.q2.QBeanSupport;
import org.jpos.util.NameRegistrar;

/**
 * While an issuer can't be reached, the switch approves small purchases in stand-in, up to the
 * limit this QBean holds. Deployed from a descriptor such as:
 *
 * <pre>
 * &lt;standin name="standin" class="com.example.practice.StandInLimit"&gt;
 *   &lt;property name="max-amount" value="${STANDIN_MAX}"/&gt;
 *   &lt;property name="currency" value="${STANDIN_CURRENCY}"/&gt;
 * &lt;/standin&gt;
 * </pre>
 */
public class StandInLimit extends QBeanSupport {
    private long maxAmount;                       // minor units: 5000 is 50.00
    private String currency;                      // ISO 4217 numeric code, such as 840 or 978

    @Override
    public void setConfiguration(Configuration cfg) throws ConfigurationException {
        super.setConfiguration(cfg);
        read(cfg);
    }

    /** Takes this descriptor's values into the fields above. */
    void read(Configuration cfg) throws ConfigurationException {
        maxAmount = cfg.getLong("max-amount");
        if (maxAmount < 0)
            throw new ConfigurationException("property 'max-amount' must not be negative");
        currency = cfg.get("currency", "840");
    }

    @Override
    protected void startService() {
        NameRegistrar.register(getName(), this);
    }

    @Override
    protected void stopService() {
        NameRegistrar.unregister(getName());
    }

    /** True when a purchase of this amount, in this currency, may be approved in stand-in. */
    public boolean allows(String currency, long amount) {
        return this.currency.equals(currency) && amount > 0 && amount <= maxAmount;
    }
}
