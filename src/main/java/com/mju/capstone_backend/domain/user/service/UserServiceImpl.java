package com.mju.capstone_backend.domain.user.service;

import com.mju.capstone_backend.domain.user.entity.User;
import com.mju.capstone_backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final Scheduler dbScheduler;

    @Override
    public Mono<Void> signup(String clerkId) {
        return Mono.fromCallable(() -> {
            if (!userRepository.existsById(clerkId)) {
                userRepository.save(User.of(clerkId));
            }
            return null;
        })
        .subscribeOn(dbScheduler)
        .then();
    }
}
