package bot.db;

import jakarta.persistence.Entity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workers")
@Getter
@Setter
@NoArgsConstructor
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="chatid", unique = true)
    private Long chatId;

    @Column(name = "in_work", nullable = false)
    private Boolean inWork;

    @Column(name="created_at")
    private LocalDateTime createdAt;

    @Column(name="dob")
    private LocalDate dob;

    @Column(name="username")
    private String username;

    @Column(name="password")
    private String password;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "first_name")
    private String firstName;

    @Column(name="patronymic")
    private String patronymic;

    @Column(name="wrote_password")
    private Boolean wrotePassword;

    @Column(name="wrote_username")
    private Boolean wroteUsername;

    @Column(name="is_registered")
    private Boolean isRegistered;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "session_id")
    private Session session;

    @OneToMany(mappedBy = "worker")
    private List<Lead> leads = new ArrayList<>();
}