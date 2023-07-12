package net.corda.samples.example.flows;
import co.paralleluniverse.fibers.Suspendable;
import net.corda.samples.example.contracts.IOUContract;
import net.corda.samples.example.states.IOUState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import java.util.Arrays;

import static net.corda.core.contracts.ContractsDSL.requireThat;
import net.corda.core.identity.CordaX500Name;

//This is the transaction between the Seller's bank and the seller, where the seller's bank issues an IOU
//to the seller
/**
 * This flow allows two parties (the [Initiator] and the [Acceptor]) to come to an agreement about the IOU encapsulated
 * within an [IOUState].
 *
 * In our simple example, the [Acceptor] always accepts a valid IOU.
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice, we would recommend splitting up the various stages of the flow into sub-routines.
 *
 * All methods called within the [FlowLogic] subclass need to be annotated with the @Suspendable annotation.
 */
public class MakePayment { // state flow contact
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final int iouValue;  //IOU Value for the transaction b/w seller bank and seller
        private final Party counterParty; //other party for first transaction is the seller
        // flow start Fow1 iouValueX: 500, sellerParty: Bank of Buyer
        private final String statusZ;

        private final Step GENERATING_TRANSACTION = new Step("Generating transaction based on new IOU.");
        private final Step VERIFYING_TRANSACTION = new Step("Verifying contract constraints.");
        private final Step SIGNING_TRANSACTION = new Step("Signing transaction with our private key.");
        private final Step GATHERING_SIGS = new Step("Gathering the Seller Party's signature.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final Step FINALISING_TRANSACTION = new Step("Obtaining notary signature and recording transaction.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        // The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
        // checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
        // function.
        private final ProgressTracker progressTracker = new ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        );

        public Initiator(int iouValue, Party counterParty, String statusZ) {
            this.iouValue = iouValue;
            this.counterParty = counterParty;

            //this.statusX= statusX;
            this.statusZ = statusZ;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            // Obtain a reference to a notary we wish to use.
            /** Explicit selection of notary by CordaX500Name - argument can be coded in flows or parsed from config (Preferred)*/
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));
            //final String statusX="Letter of Credit has been verified by both the parties.Invoice has been issued by the " +
                //    "the Bank of Seller.";
            // Stage 1.
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            // Generate an unsigned transaction.
            Party payer = getOurIdentity(); // the issuer of the iou
            IOUState iouState1 = new IOUState(iouValue, payer, counterParty, new UniqueIdentifier(), statusZ);
            final Command<IOUContract.Commands.Create> txCommand1 = new Command<>(
                    new IOUContract.Commands.Create(),
                    Arrays.asList(iouState1.getSender().getOwningKey(), iouState1.getReceiver().getOwningKey()));
            final TransactionBuilder txBuilder1 = new TransactionBuilder(notary)
                    .addOutputState(iouState1, IOUContract.ID)
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
            FlowSession sellerPartySession = initiateFlow(counterParty);
            final SignedTransaction fullySignedTx1 = subFlow(
                    new CollectSignaturesFlow(partSignedTx1, Arrays.asList(sellerPartySession), CollectSignaturesFlow.Companion.tracker()));

            // Stage 5.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(new FinalityFlow(fullySignedTx1, Arrays.asList(sellerPartySession)));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {

        private final FlowSession counterPartySession;

        public Responder(FlowSession counterPartySession) {
            this.counterPartySession = counterPartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow1 extends SignTransactionFlow {
                private SignTxFlow1(FlowSession sellerPartyFlow, ProgressTracker progressTracker) {
                    super(sellerPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require -> {
                        ContractState output1 = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be an IOU transaction.", output1 instanceof IOUState);
                        IOUState iou1 = (IOUState) output1;
                        require.using("I won't accept IOUs with a value over 1000.", iou1.getValue() <= 1000);
                        return null;
                    });
                }
            }
            final SignTxFlow1 signTxPayment = new SignTxFlow1(counterPartySession, SignTransactionFlow.Companion.tracker());
            final SecureHash txId1 = subFlow(signTxPayment).getId();

            return subFlow(new ReceiveFinalityFlow(counterPartySession, txId1));
        }
    }
}



