package org.example.server;


import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.stub.StreamObserver;
import org.example.StudentRequest;
import org.example.StudentResponse;
import org.example.StudentServceGrpc;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;


@GRpcService
public class Server extends StudentServceGrpc.StudentServceImplBase {

    Logger logger = LoggerFactory.getLogger(Server.class);

    public void getStudent(StudentRequest request, StreamObserver<StudentResponse> responseObserver) {
        Context.CancellableContext cancellableContext = Context.current().withCancellation();
        Context.CancellationListener cancellationListener = context -> {
            if (cancellableContext.isCancelled()) {
                logger.info("Request {} is cancelled", request.getId());
            }
        };
        cancellableContext.addListener(cancellationListener, MoreExecutors.directExecutor());
        for (int i = 0; i < 1000; i++) {
            StudentResponse studentResponse = StudentResponse.newBuilder()
                    .setName("Student " + request.getId() + "-" + i)
                    .setAge(ThreadLocalRandom.current().nextInt(18, 25))
                    .build();
            logger.info("RequestId={}, iteration={}", request.getId(), i);
            if (cancellableContext.isCancelled()) {
                logger.info("Cancelled client request={} when i = {}", request.getId(), i);
                return;
            }
            responseObserver.onNext(studentResponse);
        }
        responseObserver.onCompleted();
    }


}
