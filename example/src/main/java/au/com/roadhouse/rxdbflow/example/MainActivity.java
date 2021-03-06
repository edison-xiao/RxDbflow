package au.com.roadhouse.rxdbflow.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.raizlabs.android.dbflow.sql.language.Method;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper;

import java.util.List;

import au.com.roadhouse.rxdbflow.DBFlowSchedulers;
import au.com.roadhouse.rxdbflow.example.model.TestModel;
import au.com.roadhouse.rxdbflow.example.model.TestModelTwo;
import au.com.roadhouse.rxdbflow.sql.language.RxSQLite;
import au.com.roadhouse.rxdbflow.sql.transaction.RxGenericTransactionBlock;
import au.com.roadhouse.rxdbflow.sql.transaction.RxModelOperationTransaction;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends AppCompatActivity {

    private CompositeSubscription mDataSubscribers;
    private Subscription mDatabaseInitSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDataSubscribers = new CompositeSubscription();

        mDatabaseInitSubscription = RxSQLite.select(Method.count())
                                            .from(TestModel.class)
                                            .asCountObservable()
                                            .restartOnChange()
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(
                                                    new Action1<Long>() {
                                                        @Override
                                                        public void call(Long aLong) {
                                                            if (aLong == 0) {
                                                                Log.e("TEST", "call: populating database");
                                                                mDatabaseInitSubscription.unsubscribe();
                                                                populateDatabase();
                                                            } else {
                                                                Log.e("TEST", "call: clearing database");
                                                                clearDatabase();
                                                            }
                                                        }
                                                    }
                                            );
    }

    private void clearDatabase() {
        RxSQLite.delete(TestModel.class)
                .asExecuteObservable()
                .publishTableUpdates()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    private void populateList() {

        //We are listening for table changes so we add the subscription to the datasubscribers
        //so the subscription can be cleaned up when it's no longer needed
        mDataSubscribers.add(
                RxSQLite.select()
                        .from(TestModel.class)
                        .asListObservable()
                        .restartOnChange(TestModel.class, TestModelTwo.class)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<List<TestModel>>() {
                            @Override
                            public void call(List<TestModel> testModels) {
                                Log.e("TEST", "We have " + testModels.size() + " models ");
                            }
                        }));
    }

    private void populateDatabase() {
        insertWithModelOperations();
        insertWithGenericTransactionBlock();
        insertWithModelOperationTransaction();
    }

    private void insertWithModelOperations() {
        TestModel modelOne = new TestModel();
        modelOne.setFirstName("Bob");
        modelOne.setLastName("Marley");

        TestModel modelTwo = new TestModel();
        modelOne.setFirstName("John");
        modelOne.setLastName("Doe");

        TestModel modelThree = new TestModel();
        modelOne.setFirstName("Bob");
        modelOne.setLastName("Don");

        modelOne.insertAsObservable()
                .subscribeOn(DBFlowSchedulers.background())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<TestModel>() {
                    @Override
                    public void call(TestModel testModel) {
                        Log.e("TEST", "Inserting model one");
                    }
                });

        modelTwo.insertAsObservable()
                .subscribeOn(DBFlowSchedulers.background())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<TestModel>() {
                    @Override
                    public void call(TestModel testModel) {
                        Log.e("TEST", "Inserting model two");
                    }
                });

        modelThree.insertAsObservable()
                  .subscribeOn(DBFlowSchedulers.background())
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe(new Action1<TestModel>() {
                      @Override
                      public void call(TestModel testModel) {
                          Log.e("TEST", "Inserting model three");
                      }
                  });


    }

    private void insertWithModelOperationTransaction() {
        TestModel modelOne = new TestModel();
        modelOne.setFirstName("Bob");
        modelOne.setLastName("Marley");

        TestModel modelTwo = new TestModel();
        modelOne.setFirstName("John");
        modelOne.setLastName("Doe");

        TestModel modelThree = new TestModel();
        modelOne.setFirstName("Bob");
        modelOne.setLastName("Don");

        mDataSubscribers.add(new RxModelOperationTransaction.Builder(TestModel.class)
                .setDefaultOperation(RxModelOperationTransaction.MODEL_OPERATION_INSERT)
                .addModel(modelOne)
                .addModel(modelTwo)
                .addModel(modelThree)
                .build()
                .subscribeOn(DBFlowSchedulers.background())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        Log.e("TEST", "Finished inserting modelOperation models ");
                    }
                }));
    }

    private void insertWithGenericTransactionBlock() {
        //GenericTransactionBlock example
        mDataSubscribers.add(
                new RxGenericTransactionBlock.Builder(TestModel.class)
                        .addOperation(new RxGenericTransactionBlock.TransactionOperation() {
                            @Override
                            public boolean onProcess(DatabaseWrapper databaseWrapper) {
                                TestModel test = new TestModel();
                                test.setFirstName("Bob");
                                test.setLastName("Marley");
                                test.insert();
                                return true;
                            }
                        })
                        .addOperation(new RxGenericTransactionBlock.TransactionOperation() {
                            @Override
                            public boolean onProcess(DatabaseWrapper databaseWrapper) {
                                TestModel test = new TestModel();
                                test.setFirstName("John");
                                test.setLastName("Doe");
                                test.insert();

                                return true;
                            }
                        })
                        .addOperation(new RxGenericTransactionBlock.TransactionOperation() {
                            @Override
                            public boolean onProcess(DatabaseWrapper databaseWrapper) {
                                TestModel test = new TestModel();
                                test.setFirstName("Elvis");
                                test.setLastName("Presely");
                                test.insert();

                                return true;
                            }
                        }).build()
                        .subscribeOn(DBFlowSchedulers.background())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<Void>() {
                            @Override
                            public void call(Void aVoid) {
                                Log.e("TEST", "Finished inserting genericTransactionBlock models ");
                                populateList();
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                Log.e("TEST", "call: Transaction error occurred", throwable);
                            }
                        }));
    }

    @Override
    protected void onDestroy() {
        //Clean up all data subscribes
        mDataSubscribers.unsubscribe();
        SQLite.delete().from(TestModel.class).execute();
        super.onDestroy();
    }
}