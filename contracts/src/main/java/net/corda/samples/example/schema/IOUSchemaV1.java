//package net.corda.samples.example.schema;
//
//import net.corda.core.schemas.MappedSchema;
//import net.corda.core.schemas.PersistentState;
//
//import javax.persistence.Column;
//import javax.persistence.Entity;
//import javax.persistence.Table;
//import java.util.Arrays;
//import java.util.UUID;
////4.6 changes
//import org.hibernate.annotations.Type;
//import javax.annotation.Nullable;
//
///**
// * An IOUState schema.
// */
//public class IOUSchemaV1 extends MappedSchema {
//    public IOUSchemaV1() {
//        super(IOUSchema.class, 1, Arrays.asList(PersistentIOU.class));
//    }
//
//    @Nullable
//    @Override
//    public String getMigrationResource() {
//        return "iou.changelog-master";
//    }
//
//    @Entity
//    @Table(name = "iou_states")
//    public static class PersistentIOU extends PersistentState {
//        @Column(name = "sender") private final String sender;
//        @Column(name = "receiver") private final String receiver;
//        @Column(name = "value") private final int value;
//        @Column(name = "linearId") @Type (type = "uuid-char") private final UUID linearId;
//        @Column(name = "currentStatus") private final String currentStatus;
//
//
//        public PersistentIOU(String sender, String receiver, int value, UUID linearId, String currentStatus) {
//            this.sender = sender;
//            this.receiver = receiver;
//            this.value = value;
//            this.linearId = linearId;
//            this.currentStatus = currentStatus;
//        }
//
//        // Default constructor required by hibernate.
//        public PersistentIOU() {
//            this.sender = null;
//            this.receiver = null;
//            this.value = 0;
//            this.linearId = null;
//            this.currentStatus = null;
//        }
//
//        public String getSender() {
//            return sender;
//        }
//
//        public String getReceiver() {
//            return receiver;
//        }
//
//        public int getValue() {
//            return value;
//        }
//
//        public UUID getId() {
//            return linearId;
//        }
//
//        public String getCurrentStatus() {
//            return currentStatus;
//        }
//    }
//}