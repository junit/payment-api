package io.yope.payment.blockchain.bitcoinj;

import io.yope.payment.blockchain.BlockChainService;
import io.yope.payment.blockchain.BlockchainException;
import io.yope.payment.domain.Account;
import io.yope.payment.domain.Transaction;
import io.yope.payment.domain.Wallet;
import io.yope.payment.domain.transferobjects.WalletTO;
import io.yope.payment.services.AccountService;
import io.yope.payment.services.TransactionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.store.UnreadableWalletException;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bitcoinj client implementation.
 */
@Slf4j
@AllArgsConstructor
public class BitcoinjBlockchainServiceImpl implements BlockChainService {


    private NetworkParameters params;

    private BlockChain chain;

    private PeerGroup peerGroup;

    private TransactionService transactionService;

    private AccountService accountService;



    public void init(final Wallet wallet) {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            peerGroup.addPeerDiscovery(new DnsDiscovery(params));
            peerGroup.setBloomFilterFalsePositiveRate(0.00001);
            try {
                registerInBlockchain(org.bitcoinj.core.Wallet.loadFromFileStream(
                        new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(wallet.getContent()))));
                peerGroup.startAsync();
                peerGroup.downloadBlockChain();
            } catch (final UnreadableWalletException e) {
                log.error("wallet {} cannot be registered to the chain", wallet.getHash(), e);
            }
        });
    }

    @Override
    public Wallet register() throws BlockchainException {
        final org.bitcoinj.core.Wallet btcjWallet = new org.bitcoinj.core.Wallet(params);
        final DeterministicKey freshKey = btcjWallet.freshReceiveKey();
        //todo: how many confirmations does Yope wait?
        btcjWallet.allowSpendingUnconfirmedTransactions();
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            btcjWallet.saveToFileStream(outputStream);
        } catch (final IOException e) {
            throw new BlockchainException(e);
        }
        registerInBlockchain(btcjWallet);

        final WalletTO wallet = WalletTO.builder().hash(freshKey.toAddress(params).toString()).
                content(DatatypeConverter.printBase64Binary(outputStream.toByteArray())).
                privateKey(freshKey.getPrivateKeyEncoded(params).toString()).build();
        return wallet;
    }

    @Override
    public void send(final Transaction transaction) throws BlockchainException {
        try {
            final Coin value = Coin.valueOf(transaction.getAmount().longValue());
            final org.bitcoinj.core.Wallet sender = centralWallet();
            sender.allowSpendingUnconfirmedTransactions();
            registerInBlockchain(sender);
            final Address receiver = new Address(params, transaction.getDestination().getHash());
            sender.sendCoins(peerGroup, receiver, value);
        } catch (final UnreadableWalletException e) {
            throw new BlockchainException(e);
        } catch (final InsufficientMoneyException e) {
            throw new BlockchainException(e);
        } catch (final AddressFormatException e) {
            throw new BlockchainException(e);
        }
    }

    private void registerInBlockchain(final org.bitcoinj.core.Wallet wallet) {
        log.info("******** register {} in blockchain", wallet.toString());
        chain.addWallet(wallet);
        peerGroup.addWallet(wallet);
        WalletEventListener walletEventListener = new WalletEventListener(peerGroup,params,transactionService);
        wallet.addEventListener(walletEventListener);
    }

    @Override
    public String generateCentralWalletHash() throws BlockchainException {
        try {
            final org.bitcoinj.core.Wallet receiver = centralWallet();

            final DeterministicKey freshKey = receiver.freshReceiveKey();
            final String freshHash = freshKey.toAddress(params).toString();
            log.info("******** fresh hash: {}", freshHash);
            return freshHash;
        } catch (final UnreadableWalletException e) {
            log.error("error during hash generation", e);
        }
        return null;
    }

    private org.bitcoinj.core.Wallet centralWallet() throws UnreadableWalletException {
        Account admin = accountService.getByType(Account.Type.ADMIN).iterator().next();
        Wallet centralWallet = admin.getWallets().iterator().next();
        return org.bitcoinj.core.Wallet.loadFromFileStream(
                new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(centralWallet.getContent())));
    }

}
