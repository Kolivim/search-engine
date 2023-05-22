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
            for (Iterator<RunnableFuture<Boolean>> futureIterator = runnableFutureList.iterator(); futureIterator.hasNext(); )
            {
                RunnableFuture<Boolean> future = futureIterator.next();
                if (future.isDone())
                {
                    try
                    {
                        System.out.println("Задача выполнилась за: " + future.get() + " миллисекунд");
                        futureIterator.remove();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }

                if (false)  // TODO: Остановка кнопкой
                {
                    future.cancel(true);
                    System.out.println("Индексация остановлена пользователем");
                }
            }
    }
}
