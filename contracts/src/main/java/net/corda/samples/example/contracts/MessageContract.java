package net.corda.samples.example.contracts;

import net.corda.samples.example.states.MessageState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;

import net.corda.core.transactions.LedgerTransaction;



import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * An implementation of a basic smart contract in Corda.
 *
 * This contracts enforces rules regarding the creation of a valid [IOUState], which in turn encapsulates an [IOU].
 *
 * For a new [IOU] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output states: the new [IOU].
 * - An Create() command with the public keys of both the lender and the borrower.
 *
 * All contracts must subclass the [Contract] interface.
 */
public class MessageContract implements Contract {
    public static final String ID = "net.corda.samples.example.contracts.MessageContract";

    /**
     * The "verify()" function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<Commands.Create> command = requireSingleCommand(tx.getCommands(), Commands.Create.class);
        requireThat(require -> {
            // Generic constraints around the Message transaction.
            require.using("No inputs should be consumed when sending a message.",
                    tx.getInputs().isEmpty());
            require.using("Only one output states should be created.",
                    tx.getOutputs().size() == 1);
            final MessageState out = tx.outputsOfType(MessageState.class).get(0);
            require.using("The Sender and the Receiver cannot be the same entity.",
                    !out.getMsgSender().equals(out.getMsgReceiver()));
//            require.using("The status of the transaction must be returned.",
//                    out.getMsgUpdate()!=null);





            return null;
        });
    }

    /**
     * This contracts only implements one command, Create.
     */
    public interface Commands extends CommandData {
        class Create implements Commands {}
    }
}