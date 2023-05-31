package searchengine.services;

import lombok.NoArgsConstructor;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;


public class ResultCheckerParse implements Runnable
{
//    private final List<RunnableFuture<Boolean>> runnableFutureList;
    private final Stack<RunnableFuture<Boolean>> runnableFutureList;
    private boolean isIndexingStopped;  //1
    public void setIndexingStopped(boolean indexingStopped) {this.isIndexingStopped = indexingStopped;}  //1
    public boolean isIndexingStopped() {return isIndexingStopped;}  //1
    private IndexingService indexingService; //1
    public ResultCheckerParse(
//                                List<RunnableFuture<Boolean>> runnableFutureList
                                Stack<RunnableFuture<Boolean>> runnableFutureList
                                )
    {
//        this.runnableFutureList = (Stack<RunnableFuture<Boolean>>)SpringUtils.ctx.getBean("taskList", IndexingService.class);
                this.runnableFutureList = runnableFutureList;
//        this.isIndexingStopped = false;  //1
        this.indexingService = (IndexingService) SpringUtils.ctx.getBean(IndexingService.class);    //1
    }
    @Override
    public void run()
    {


            while (runnableFutureList.size() != 0)    // while с проверкой условий по остановке потока - т.е. завершению всех задач
            {
                for (Iterator<RunnableFuture<Boolean>> futureIterator = runnableFutureList.iterator(); futureIterator.hasNext(); ) {
                    RunnableFuture<Boolean> future = futureIterator.next();
                    if (future.isDone()) // || future.isCancelled()
                    {
//                        try {
//                            System.out.println("\nЗадача выполнилась : " + future.get());
                            futureIterator.remove();
//                        } catch (InterruptedException | ExecutionException e) {
//                            System.err.println("В классе ResultCheckerParse сработал InterruptedException | ExecutionException ///1 " + e.getMessage() + " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed() + " ///4 " + e.getCause() + " ///5 " + e.getLocalizedMessage() + " ///6 " + e.getClass());
//                        }
                    }

                    if (isIndexingStopped)  // TODO: Остановка кнопкой - реализовать    //28m
                    {
//                        try {   //try1
                        boolean cancelled = future.cancel(true);
                        System.out.println("\nИндексация остановлена пользователем: " + cancelled);
//                        } catch(CancellationException e) { System.err.println("В классе ResultCheckerParse сработал CancellationException ///1 " + e.getMessage() + " ///2 " + e.getStackTrace() + " ///3 " + e.getSuppressed() + " ///4 " + e.getCause() + " ///5 " + e.getLocalizedMessage() + " ///6 " + e.getClass());} // try1
                    }
                }
            }
            indexingService.setIndexingStarted(false);  //1

        System.out.println("\nЗавершение метода run() класса ResultCheckerParse, runnableFutureList.size()=" + runnableFutureList.size());
    }
}
