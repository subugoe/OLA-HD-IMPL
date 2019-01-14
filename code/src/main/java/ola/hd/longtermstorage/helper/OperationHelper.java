package ola.hd.longtermstorage.helper;

public class OperationHelper {

    public static void doWithRetry(int maxAttempt, Operation operation) throws Exception {
        int attempt = 0;

        while (true) {
            try {
                operation.run();
                break;
            } catch (Exception e) {
                attempt++;

                if (attempt < maxAttempt) {
                    System.err.println("Could not import data. Number of attempt: " + attempt);
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
