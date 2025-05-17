package com.walletapi.entities

import com.walletapi.models.User
import jakarta.persistence.*

@Entity
class UserEntity(
    @Column(nullable = false) var fullName: String,
    @Column(nullable = false, unique = true) var email: String,
    @Column(nullable = false) var password: String,
    @OneToMany(
        mappedBy = "user",
        cascade = [CascadeType.ALL],
        fetch = FetchType.LAZY
    ) var wallets: MutableList<WalletEntity>
) {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(unique = true, nullable = false)
    var id: Long? = null


    fun toUser(): User {
        return User(
            fullName = this.fullName ?: "",
            email = this.email ?: "",
            password = this.password ?: "",
            wallets = this.wallets.map { it.toWallet() }
        )
    }
}

fun userToEntity(user: User): UserEntity {
    var generatedUser =  UserEntity(
        fullName = user.fullName,
        email = user.email,
        password = user.password,
        wallets = mutableListOf()
    )

    generatedUser.wallets = generatedUser.wallets.map { walletToEntity(it.toWallet(), generatedUser) }.toMutableList()
    return generatedUser
}