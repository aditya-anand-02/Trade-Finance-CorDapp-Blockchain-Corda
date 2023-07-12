package net.corda.samples.example.states;
import net.corda.core.contracts.ContractState;
import net.corda.samples.example.contracts.MessageContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.Arrays;
import java.util.List;
@BelongsToContract(MessageContract.class)
public class MessageState implements ContractState, LinearState {
    private final String msgUpdate;
    private final Party msgSender;
    private final Party msgReceiver;
    private final UniqueIdentifier linearId;


    public MessageState(String msgUpdate, Party msgSender, Party msgReceiver, UniqueIdentifier linearId) {
        this.msgUpdate = msgUpdate;
        this.msgSender = msgSender;
        this.msgReceiver = msgReceiver;
        this.linearId = linearId;

    }

    public String getMsgUpdate() {
        return msgUpdate;
    }



    public Party getMsgSender() {
        return msgSender;
    }



    public Party getMsgReceiver() {
        return msgReceiver;
    }

    @Override
    public UniqueIdentifier getLinearId() { return linearId; }



    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(msgSender, msgReceiver);
    }

    @Override
    public String toString() {
        return "MessageState{" +
                "msgUpdate='" + msgUpdate + '\'' +
                ", msgSender=" + msgSender +
                ", msgReceiver=" + msgReceiver +
                ", uniqId=" + linearId +
                '}';
    }

    //    public void setMsgUpdate(String msgUpdate) {
//        this.msgUpdate = msgUpdate;
//    }
//
//    public void setMsgSender(Party msgSender) {
//        this.msgSender = msgSender;
//    }
//
//    public void setMsgReceiver(Party msgReceiver) {
//        this.msgReceiver = msgReceiver;
//    }

}
