/**
 *
 */
package io.yope.payment.neo4j.domain;

import java.math.BigDecimal;

import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;

import io.yope.payment.domain.Wallet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author mgerardi
 *
 */
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString(of = {"name", "balance", "availableBalance"})
@NodeEntity
public class Neo4JWallet implements Wallet {

    @GraphId
    private Long id;

    @Indexed
    private String name;

    private String walletHash;

    private BigDecimal balance;

    private BigDecimal availableBalance;

    private Status status;

    private String description;

    private Long creationDate;

    private Long modificationDate;

    private Type type;

    private String content;

    private String privateKey;

    public static Neo4JWallet.Neo4JWalletBuilder from(final Wallet wallet) {
        return Neo4JWallet.builder()
                .balance(wallet.getBalance())
                .availableBalance(wallet.getAvailableBalance())
                .type(wallet.getType())
                .creationDate(wallet.getCreationDate())
                .modificationDate(wallet.getModificationDate())
                .description(wallet.getDescription())
                .walletHash(wallet.getWalletHash())
                .id(wallet.getId())
                .status(wallet.getStatus())
                .name(wallet.getName())
                .privateKey(wallet.getPrivateKey())
                .content(wallet.getContent());
    }

}
