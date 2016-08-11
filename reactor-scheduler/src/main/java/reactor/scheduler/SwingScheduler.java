/*
 * Copyright (c) 2011-2016 Pivotal Software, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package reactor.scheduler;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import reactor.core.Cancellation;
import reactor.core.publisher.Operators;
import reactor.core.scheduler.TimedScheduler;
import reactor.core.Exceptions;

/** 
 * Scheduler that runs tasks on Swing's event dispatch thread. 
 */
public final class SwingScheduler implements TimedScheduler {

	@Override
	public Cancellation schedule(Runnable task) {
        SwingScheduledDirectAction a = new SwingScheduledDirectAction(task);

        SwingUtilities.invokeLater(a);
        
        return a;
	}
	
	@Override
	public Cancellation schedule(Runnable task, long delay, TimeUnit unit) {
	    if (delay <= 0) {
	        return schedule(task);
	    }

        Timer timer = new Timer((int)unit.toMillis(delay), null);
        timer.setRepeats(false);
        timer.addActionListener(e -> {
            try {
                task.run();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
	            Operators.onErrorDropped(ex);
            }
        });
        timer.start();
        return timer::stop;
	}
	
	@Override
	public Cancellation schedulePeriodically(Runnable task, long initialDelay, long period, TimeUnit unit) {
        Timer timer = new Timer((int)unit.toMillis(period), null);
        timer.setInitialDelay((int)unit.toMillis(initialDelay));
        
        timer.addActionListener(e -> {
            try {
                task.run();
            } catch (Throwable ex) {
                timer.stop();
                Exceptions.throwIfFatal(ex);
	            Operators.onErrorDropped(ex);
            }
        });
        timer.start();
        return timer::stop;
	}
	
	@Override
	public TimedWorker createWorker() {
		return new SwingSchedulerWorker();
	}

	static final class SwingSchedulerWorker implements TimedScheduler.TimedWorker {

		volatile boolean unsubscribed;

		Set<Timer> tasks;
		
		public SwingSchedulerWorker() {
		    this.tasks = new HashSet<>();
		}
		
		@Override
		public void shutdown() {
			if (unsubscribed) {
				return;
			}
			unsubscribed = true;
			
			Set<Timer> set;
			synchronized (this) {
			    set = tasks;
			    tasks = null;
			}
			
			if (set != null) {
			    for (Timer t : set) {
			        t.stop();
			    }
			}
		}
		
		void remove(Timer timer) {
            if (unsubscribed) {
                return;
            }
            synchronized (this) {
                if (unsubscribed) {
                    return;
                }
                
                tasks.remove(timer);
            }
		}

		@Override
		public Cancellation schedule(Runnable action) {
		    
		    if (!unsubscribed) {
    		    SwingScheduledDirectAction a = new SwingScheduledDirectAction(action);
    
    	        SwingUtilities.invokeLater(a);
    	        
    	        return a;
		    }
		    return REJECTED;
		}

		@Override
		public Cancellation schedule(Runnable action, long delayTime, TimeUnit unit) {
            if (delayTime <= 0) {
                return schedule(action);
            }

            if (unsubscribed) {
                return REJECTED;
            }

            Timer timer = new Timer((int)unit.toMillis(delayTime), null);
            timer.setRepeats(false);
            
            synchronized (this) {
                if (unsubscribed) {
                    return REJECTED;
                }
                tasks.add(timer);
            }
            
            timer.addActionListener(e -> {
                try {
                    try {
                        action.run();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
	                    Operators.onErrorDropped(ex);
                    }
                } finally {
                    remove(timer);
                }
            });
            
            timer.start();

            if (unsubscribed) {
                timer.stop();
                return REJECTED;
            }
            
            return () -> {
                timer.stop();
                remove(timer);
            };
		}
		
		@Override
		public Cancellation schedulePeriodically(Runnable task, long initialDelay, long period, TimeUnit unit) {
		    if (unsubscribed) {
                return REJECTED;
            }

            Timer timer = new Timer((int)unit.toMillis(period), null);
            timer.setInitialDelay((int)unit.toMillis(initialDelay));
            
            synchronized (this) {
                if (unsubscribed) {
                    return REJECTED;
                }
                tasks.add(timer);
            }
            
            timer.addActionListener(e -> {
                try {
                    task.run();
                } catch (Throwable ex) {
                    timer.stop();
                    remove(timer);
                    Exceptions.throwIfFatal(ex);
	                Operators.onErrorDropped(ex);
                }
            });
            
            timer.start();

            if (unsubscribed) {
                timer.stop();
                return REJECTED;
            }
            
            return () -> {
                timer.stop();
                remove(timer);
            };
		}
	}
	
    static final class SwingScheduledDirectAction
    extends AtomicBoolean implements Runnable, Cancellation {
        /** */
        private static final long serialVersionUID = 2378266891882031635L;
        
        final Runnable action;

        public SwingScheduledDirectAction(Runnable action) {
            this.action = action;
        }
        
        @Override
        public void run() {
            if (!get()) {
                try {
                    action.run();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
	                Operators.onErrorDropped(ex);
                }
            }
        }
        
        @Override
        public void dispose() {
            set(true);
        }
    }

    static final class SwingScheduledAction
    extends AtomicBoolean implements Runnable, Cancellation {
        /** */
        private static final long serialVersionUID = 2378266891882031635L;
        
        final Runnable action;
        
        final SwingSchedulerWorker parent;

        public SwingScheduledAction(Runnable action, SwingSchedulerWorker parent) {
            this.action = action;
            this.parent = parent;
        }
        
        @Override
        public void run() {
            if (!get() && !parent.unsubscribed) {
                try {
                    action.run();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
	                Operators.onErrorDropped(ex);
                }
            }
        }
        
        @Override
        public void dispose() {
            set(true);
        }
    }

}