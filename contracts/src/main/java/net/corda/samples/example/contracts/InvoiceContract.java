package net.corda.samples.example.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;

import net.corda.core.transactions.LedgerTransaction;

import net.corda.samples.example.states.InvoiceState;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class InvoiceContract implements Contract{
    public static final String ID = "net.corda.samples.example.contracts.InvoiceContract";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<InvoiceContract.Commands.Create> command = requireSingleCommand(tx.getCommands(), InvoiceContract.Commands.Create.class);
        requireThat(require -> {
            // Generic constraints around the IOU transaction.
            require.using("No inputs should be consumed when issuing an IOU.",
                    tx.getInputs().isEmpty());
            require.using("Only one output states should be created.",
                    tx.getOutputs().size() == 1);
            final InvoiceState out = tx.outputsOfType(InvoiceState.class).get(0);
            require.using("The Sender and the Receiver cannot be the same entity.",
                    !out.getSender().equals(out.getReceiver()));



            // IOU-specific constraints.
            require.using("The IOU's value must be a non-negative value.",
                    out.getValue() > 0);


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
