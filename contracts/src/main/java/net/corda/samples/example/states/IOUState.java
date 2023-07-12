package net.corda.samples.example.states;

import net.corda.core.contracts.ContractState;
import net.corda.samples.example.contracts.IOUContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;


import java.util.Arrays;
import java.util.List;

/**
 * The states object recording IOU agreements between two parties.
 *
 * A states must implement [ContractState] or one of its descendants.
 */
@BelongsToContract(IOUContract.class)
public class IOUState implements LinearState, ContractState {
    private final Integer value;
    private final Party sender;
    private final Party receiver;
    private final UniqueIdentifier linearId;
    private final String currentStatus;

    /**
     * @param value the value of the IOU.
     * @param sender the party issuing the IOU.
     * @param receiver the party receiving and approving the IOU.
     * @param linearId the unique identifier of the IOU state.
     * @param currentStatus the status of the LC and transaction
     */
    public IOUState(Integer value,
                    Party sender,
                    Party receiver,
                    UniqueIdentifier linearId, String currentStatus)
    {
        this.value = value;
        this.sender = sender;
        this.receiver = receiver;
        this.linearId = linearId;
        this.currentStatus = currentStatus;
    }

    public Integer getValue() { return value; }
    public Party getSender() { return sender; }
    public Party getReceiver() { return receiver; }

    public String getCurrentStatus() {
        return currentStatus;
    }

    @Override public UniqueIdentifier getLinearId() { return linearId; }
    @Override public List<AbstractParty> getParticipants() {
        return Arrays.asList(sender, receiver);
    }

//    @Override public PersistentState generateMappedObject(MappedSchema schema) {
//        if (schema instanceof IOUSchemaV1) {
//            return new IOUSchemaV1.PersistentIOU(
//                    this.sender.getName().toString(),
//                    this.receiver.getName().toString(),
//                    this.value,
//                    this.linearId.getId(), this.currentStatus.toString());
//        } else {
//            throw new IllegalArgumentException("Unrecognised schema $schema");
//        }
//    }

 //   @Override public Iterable<MappedSchema> supportedSchemas() {
 //       return Arrays.asList(new IOUSchemaV1());
    //}

    @Override
    public String toString() {
        return String.format("IOUState(IOU value=%s, sender=%s, receiver=%s, linearId=%s, status=%s)", value, sender, receiver, linearId, currentStatus);
    }
}