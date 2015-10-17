/**
 *
 */
package io.yope.payment.domain;

import java.math.BigDecimal;

/**
 * @author mgerardi
 *
 */
public interface Transaction {

    /**
     * the status of the transaction.
     * @author mgerardi
     *
     */
    public enum Status {
        /**
         * transaction waiting to be processed.
         */
        PENDING,
        /**
         * transaction failed.
         */
        FAILED,
        /**
         * transaction expired as not completed after a given time interval.
         */
        EXPIRED,
        /**
         * transaction rejected either by external or by internal blockchain.
         */
        DENIED,
        /**
         * transaction accepted into the queue but waiting to be processed by external blockchain.
         */
        ACCEPTED,
        /**
         * transaction completed; amount has been passed from a wallet to another.
         */
        COMPLETED
    }

    public enum Direction {
        BOTH,
        OUT,
        IN
    }

    public enum Type {
        DEPOSIT,
        WITHDRAW,
        TRANSFER
    }

    Long getId();

    String getTransactionHash();

    String getSenderHash();

    Wallet getSource();

    Wallet getDestination();

    String getReference();

    Status getStatus();

    String getDescription();

    BigDecimal getAmount();

    Long getCreationDate();

    Long getAcceptedDate();

    Long getDeniedDate();

    Long getExpiredDate();

    Long getCompletedDate();

    Type getType();

    String getQR();
}
