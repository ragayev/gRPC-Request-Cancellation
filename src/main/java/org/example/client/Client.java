package org.example.client;

import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.example.StudentRequest;
import org.example.StudentResponse;
import org.example.StudentServceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class Client {

    static Logger logger = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) {

        //we create two contexts, because we want to check the operation for two different clients
        Context.CancellableContext firstContext = Context.current().withCancellation();
        Context.CancellableContext secondContext = Context.current().withCancellation();

        try {
            //in this runnable we cancel the request
            Runnable firstRunnable = () -> {
                ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("localhost", 6565).usePlaintext().build();
                StudentServceGrpc.StudentServceBlockingStub blockingStub = StudentServceGrpc.newBlockingStub(managedChannel);
                StudentRequest request = StudentRequest.newBuilder().setId(1).build();
                Iterator<StudentResponse> student = blockingStub.getStudent(request);
                try {
                    while (student.hasNext()) {
                        logger.info("Cancelling the current call");
                        firstContext.cancel(new RuntimeException("Exception thrown for requestId=" + 1)); // we delibertly cancel the request here
                        StudentResponse response = student.next();
                        logger.info("RequestId={}, student info [Name ={}, Age = {}]", 1, response.getName(), response.getAge());
                    }
                } catch (Exception e) {
                    if (firstContext.isCancelled()) {
                        logger.info("The call is cancelled by the client");
                    }
                    logger.error("Request encountered an exception", e);
                }

            };

            //this runnable should work until the end
            Runnable secondRunnable = () -> {
                ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("localhost", 6565).usePlaintext().build();
                StudentServceGrpc.StudentServceBlockingStub blockingStub = StudentServceGrpc.newBlockingStub(managedChannel);
                StudentRequest request = StudentRequest.newBuilder().setId(2).build();
                Iterator<StudentResponse> student = blockingStub.getStudent(request);
                while (student.hasNext()) {
                    StudentResponse response = student.next();
                    logger.info("RequestId={}, student info [Name ={}, Age = {}]", 2, response.getName(), response.getAge());
                }
            };
            firstContext.run(firstRunnable);
            secondContext.run(secondRunnable);
        } finally {
            firstContext.close();
            secondContext.close();
        }
    }
}



