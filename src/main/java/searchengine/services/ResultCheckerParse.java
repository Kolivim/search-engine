package searchengine.services;

import lombok.NoArgsConstructor;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;


public class ResultCheckerParse implements Runnable
{
    private final List<RunnableFuture<Boolean>> runnableFutureList;

    public ResultCheckerParse(List<RunnableFuture<Boolean>> runnableFutureList)
    {
        this.runnableFutureList = runnableFutureList;
    }


    @Override
    public void run()
    {

        while (runnableFutureList.size() !=0)    // while с проверкой условий по остановке потока - т.е. завершению всех задач
        {
            for (Iterator<RunnableFuture<Boolean>> futureIterator = runnableFutureList.iterator(); futureIterator.hasNext(); ) {
                RunnableFuture<Boolean> future = futureIterator.next();
                if (future.isDone()) {
                    try
                    {
                        System.out.println("\nЗадача выполнилась : " + future.get());
                        futureIterator.remove();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }

                if (false)  // TODO: Остановка кнопкой - реализовать
                {
                    future.cancel(true);
                    System.out.println("Индексация остановлена пользователем");
                }
            }
        }
        System.out.println("\nЗавершение метода run() класса ResultCheckerParse, runnableFutureList.size()=" + runnableFutureList.size());
    }
}
