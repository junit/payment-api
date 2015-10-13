package io.yope.payment.blockchain.bitcoinj;


import com.google.common.collect.Sets;
import io.yope.payment.blockchain.BlockChainService;
import io.yope.payment.blockchain.BlockchainException;
import io.yope.payment.domain.Account;
import io.yope.payment.domain.Wallet;
import io.yope.payment.domain.transferobjects.AccountTO;
import io.yope.payment.domain.transferobjects.WalletTO;
import io.yope.payment.services.AccountService;
import io.yope.payment.services.WalletService;
import lombok.extern.slf4j.Slf4j;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@Slf4j
@Configuration
@EnableAutoConfiguration
public class BitcoinjConfiguration {

    private static final String ADMIN_EMAIL = "wallet@yope.io";
    private static final String CENTRAL_WALLET_PATH = "centralwallet";
    @Autowired
    private Environment environment;

    @Bean
    public Context getContext(final NetworkParameters params ) {
        return new Context(params);
    }

    @Bean
    public BlockChain getChain(final NetworkParameters params,
                               final SPVBlockStore blockStore,
                               final Context context)
            throws BlockStoreException {
        return new BlockChain(context, blockStore);
    }

    @Bean
    public SPVBlockStore getBlockStore(final NetworkParameters params)
            throws BlockStoreException {
        final String blockstore = params instanceof TestNet3Params ? "tbtc_blockstore" : "main_blockstore";

        return new SPVBlockStore(params,
                new File(blockstore));
    }


    @Bean
    public PeerGroup getPeers(final NetworkParameters params,final BlockChain chain) {
        return new PeerGroup(params, chain);
    }

    @Bean
    public BlockChainService getBlockchainService(final NetworkParameters params,
                                                  final BlockChain blockChain,
                                                  final PeerGroup peerGroup,
                                                  final WalletService walletService,
                                                  final AccountService accountService
                                                  ){

        final BitcoinjBlockchainServiceImpl blockChainService =
                new BitcoinjBlockchainServiceImpl(params, blockChain, peerGroup);

        Wallet central = null;
        Account admin = accountService.getByEmail(ADMIN_EMAIL);
        if (admin == null) {
            try {
                Wallet inBlockChain = blockChainService.register();
                central = WalletTO.builder()
                        .content(inBlockChain.getContent())
                        .hash(inBlockChain.getHash())
                        .type(Wallet.Type.EXTERNAL)
                        .status(Wallet.Status.ACTIVE)
                        .name("central")
                        .description("central")
                        .balance(BigDecimal.ZERO)
                        .build();
                Account adm = AccountTO.builder()
                        .email(ADMIN_EMAIL)
                        .firstName("admin")
                        .lastName("admin")
                        .type(Account.Type.ADMIN)
                        .wallets(Sets.newLinkedHashSet())
                        .build();
                accountService.create(adm, central);

            } catch (BlockchainException e) {
                log.error("error during blockchain registration", e);
            }
        } else {
            central = admin.getWallets().iterator().next();
        }
        blockChainService.init(central);
        log.info("central wallet hash: {}", central.getHash());
        return blockChainService;
    }

}