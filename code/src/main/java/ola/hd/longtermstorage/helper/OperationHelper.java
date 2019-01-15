package ola.hd.longtermstorage.helper;

public class OperationHelper {

    public static String doWithRetry(int maxAttempt, Operation operation) throws Exception {
        int attempt = 0;

        while (true) {
            try {
                return operation.run();
            } catch (Exception e) {
                attempt++;

                if (attempt < maxAttempt) {
                    System.err.println("Operation failed. Number of attempt: " + attempt);
                    System.err.println("Retrying...");
                } else {
                    System.err.println("Max attempt reached. Throwing exception...");
                    System.err.println(e.getMessage());
                    throw e;
                }
            }
        }
    }
}
