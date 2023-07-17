package net.corda.samples.example.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.samples.example.contracts.InvoiceContract;
import net.corda.samples.example.states.InvoiceState;
import net.corda.core.flows.InitiatingFlow;

import java.util.Arrays;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class SendInvoice {
    @InitiatingFlow
    @StartableByRPC
    public static class InitiatorA extends FlowLogic<SignedTransaction> {
        private final Party receivingParty;
        private final int amount;

        private final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating transaction.");
        private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying Initiator contract constraints.");
        private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
        private final ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering the other Party's signature.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        private final ProgressTracker progressTracker = new ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        );

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }



        public InitiatorA(int amount, Party receivingParty) {

            this.receivingParty = receivingParty;
            this.amount= amount;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            Party sendingParty = getOurIdentity();
            InvoiceState invoiceState = new InvoiceState(amount, sendingParty, receivingParty, new UniqueIdentifier());
            final Command<InvoiceContract.Commands.Create> txCommand1 = new Command<>(
                    new InvoiceContract.Commands.Create(),
                    Arrays.asList(invoiceState.getSender().getOwningKey(), invoiceState.getReceiver().getOwningKey()));
            final TransactionBuilder txBuilder1 = new TransactionBuilder(notary)
                    .addOutputState(invoiceState, InvoiceContract.ID)
                    .addCommand(txCommand1);
            //status update
            // Stage 2.
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            // Verify that the transaction is valid.
            txBuilder1.verify(getServiceHub());

            // Stage 3.sell
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            // Sign the transaction.
            final SignedTransaction partSignedTx1 = getServiceHub().signInitialTransaction(txBuilder1);

            // Stage 4.
            progressTracker.setCurrentStep(GATHERING_SIGS);
            // Send the state to the Seller Party, and receive it back with their signature.
            FlowSession receiverPartySession = initiateFlow(receivingParty);
            final SignedTransaction fullySignedTx1 = subFlow(
                    new CollectSignaturesFlow(partSignedTx1, Arrays.asList(receiverPartySession), CollectSignaturesFlow.Companion.tracker()));

            // Stage 5.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(new FinalityFlow(fullySignedTx1, Arrays.asList(receiverPartySession)));
        }
    }

    @InitiatedBy(SendInvoice.InitiatorA.class)
    public static class ResponderB extends FlowLogic<SignedTransaction> {
        //private variable
        private final FlowSession receivingPartySession;

        //Constructor
        public ResponderB(FlowSession receivingPartySession) {
            this.receivingPartySession = receivingPartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            class SignTxFlow1 extends SignTransactionFlow {
                private SignTxFlow1(FlowSession sellerPartyFlow, ProgressTracker progressTracker) {
                    super(sellerPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require -> {
                        ContractState output1 = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be an Invoice transaction.", output1 instanceof InvoiceState);


                        return null;
                    });
                }
            }
            final SignTxFlow1 signTxPayment = new SignTxFlow1(receivingPartySession, SignTransactionFlow.Companion.tracker());
            final SecureHash txId1 = subFlow(signTxPayment).getId();

            return subFlow(new ReceiveFinalityFlow(receivingPartySession, txId1));
        }

    }
}
